package cinnamon.render;

import cinnamon.model.Vertex;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import cinnamon.utils.TextUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static cinnamon.Client.LOGGER;
import static cinnamon.model.GeometryHelper.quad;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Font {

    //texture
    private static final int TEXTURE_W = 512, TEXTURE_H = 512;

    //properties
    private static final float Z_OFFSET = 0.01f;
    public static final float Z_DEPTH = Z_OFFSET * 3;
    private static final int
            SHADOW_COLOR = 0xFF161616,
            BG_COLOR = 0x44 << 24,
            SHADOW_OFFSET = 1,
            BOLD_OFFSET = 1,
            ITALIC_OFFSET = 3;

    private static final Random RANDOM = new Random();
    private static int SEED = 42;

    //buffers
    private final STBTTAlignedQuad q = STBTTAlignedQuad.malloc();
    private final FloatBuffer xb = memAllocFloat(1), yb = memAllocFloat(1);

    //data
    private final ByteBuffer ttf; //needs to be kept in memory
    private final STBTTFontinfo info = STBTTFontinfo.malloc();
    private final STBTTPackedchar.Buffer charData;
    private final int textureID;

    //properties
    public final float
            lineHeight,
            lineGap,
            ascent,
            descent,
            scale;

    private final float[] missing;
    private final Map<Float, List<Integer>> charsByWidth = new HashMap<>();


    // -- font initialization -- //


    public Font(Resource res, float height) {
        this(res, height, 1, false);
    }

    public Font(Resource res, float height, float lineSpacing, boolean smooth) {
        this.ttf = IOUtils.getResourceBuffer(res);
        this.lineHeight = height;
        this.lineGap = lineSpacing;
        this.charData = STBTTPackedchar.malloc(0x10FFFF + 3 + 1); //".notdef" ".null" "nonmarkingreturn"
        this.textureID = glGenTextures();

        //font data
        stbtt_InitFont(info, ttf);

        IntBuffer ascent = memAllocInt(1);
        IntBuffer descent = memAllocInt(1);
        IntBuffer lineGap = memAllocInt(1);

        stbtt_GetFontVMetrics(info, ascent, descent, lineGap);

        this.scale = height / (ascent.get(0) - descent.get(0));
        this.ascent = ascent.get(0) * scale;
        this.descent = descent.get(0) * scale;

        memFree(ascent); memFree(descent); memFree(lineGap);

        //font bitmap
        try (STBTTPackContext spc = STBTTPackContext.malloc()) {
            ByteBuffer bitmap = BufferUtils.createByteBuffer(TEXTURE_W * TEXTURE_H);

            stbtt_PackBegin(spc, bitmap, TEXTURE_W, TEXTURE_H, 0, 1, NULL);
            stbtt_PackFontRange(spc, ttf, 0, height, 0, charData);
            stbtt_PackEnd(spc);

            glBindTexture(GL_TEXTURE_2D, textureID);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, smooth ? GL_LINEAR : GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, smooth ? GL_LINEAR : GL_NEAREST);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, TEXTURE_W, TEXTURE_H, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);

            glBindTexture(GL_TEXTURE_2D, 0);
        }

        //char widths
        getCharData(0x110000); //".notdef"
        this.missing = new float[]{q.s0(), q.s1(), q.t0(), q.t1()};

        for (int i = 0; i < 0x10FFFF; i++) {
            if (Character.isSpaceChar(i) || isMissingGlyph(i))
                continue;
            float w = width(i);
            charsByWidth.computeIfAbsent(w, k -> new ArrayList<>()).add(i);
        }

        //finished!
        LOGGER.debug("Loaded font \"{}\"", res);
    }

    public void free() {
        glDeleteTextures(textureID);
        charData.free();
        info.free();
        q.free();
        memFree(xb);
        memFree(yb);
    }

    private void getCharData(int c) {
        stbtt_GetPackedQuad(charData, TEXTURE_W, TEXTURE_H, c, xb, yb, q, false);
    }


    // -- font rendering -- //


    public void render(VertexConsumer consumer, MatrixStack matrices, float x, float y, Text text) {
        render(consumer, matrices, x, y, text, Alignment.LEFT);
    }

    public void render(VertexConsumer consumer, MatrixStack matrices, float x, float y, Text text, Alignment alignment) {
        render(consumer, matrices, x, y, text, alignment, 1);
    }

    public void render(VertexConsumer consumer, MatrixStack matrices, float x, float y, Text text, Alignment alignment, int indexScaling) {
        List<Text> list = TextUtils.split(text, "\n");

        for (int i = 0; i < list.size(); i++) {
            Text t = list.get(i);
            int x2 = Math.round(alignment.getOffset(this.width(t)));
            int y2 = Math.round((lineHeight * (i + 1) + descent));
            bake(consumer, matrices, t, x + x2, y + y2, indexScaling * Z_OFFSET);
        }
    }

    private void bake(VertexConsumer consumer, MatrixStack matrices, Text text, float x, float y, float zOffset) {
        //prepare vars
        boolean[] prevItalic = {false};
        xb.put(0, 0f); yb.put(0, 0f);
        SEED = RANDOM.nextInt();

        //iterate text and children
        text.visit((s, style) -> {
            if (s.isEmpty())
                return;

            //style flags
            boolean italic  = style.isItalic();
            boolean bold    = style.isBold();
            boolean obf     = style.isObfuscated();
            boolean shadow  = style.hasShadow();
            boolean outline = style.hasOutline();
            boolean bg      = style.hasBackground();
            boolean under   = style.isUnderlined();
            boolean strike  = style.isStrikethrough();
            int zi = (shadow ? 1 : 0) + (outline ? 1 : 0) + (bg ? 1 : 0);

            if (prevItalic[0] && !italic)
                xb.put(0, xb.get(0) + ITALIC_OFFSET);
            prevItalic[0] = italic;

            //backup initial buffer
            float initialX = xb.get(0), initialY = yb.get(0);

            //main render
            int color = Objects.requireNonNullElse(style.getColor(), -1);
            bakeString(consumer, matrices, s, italic, bold, obf, under, strike, x, y, zOffset * zi--, color);

            //backup final buffer
            float finalX = xb.get(0), finalY = yb.get(0);

            //render shadow
            if (shadow) {
                xb.put(0, initialX + SHADOW_OFFSET); yb.put(0, initialY + SHADOW_OFFSET);
                int sc = Objects.requireNonNullElse(style.getShadowColor(), SHADOW_COLOR);
                bakeString(consumer, matrices, s, italic, bold, obf, under, strike, x, y, zOffset * zi--, sc);
            }

            //render outline
            if (outline) {
                int oc = Objects.requireNonNullElse(style.getOutlineColor(), SHADOW_COLOR);
                for (int i = -SHADOW_OFFSET; i <= SHADOW_OFFSET; i += SHADOW_OFFSET) {
                    for (int j = -SHADOW_OFFSET; j <= SHADOW_OFFSET; j += SHADOW_OFFSET) {
                        xb.put(0, initialX + i); yb.put(0, initialY + j);
                        bakeString(consumer, matrices, s, italic, bold, obf, under, strike, x, y, zOffset * zi, oc);
                    }
                }
                zi--;
            }

            //render background
            if (bg) {
                float x0 = initialX + x;
                float y0 = initialY + y - lineHeight - descent;
                float w  = finalX - initialX;
                float h  = lineHeight;

                if (italic)
                    w += ITALIC_OFFSET;

                if (outline) {
                    x0--; y0--;
                    w += 2; h += 2;
                } else if (shadow) {
                    w++; h++;
                }

                int bgc = Objects.requireNonNullElse(style.getBackgroundColor(), BG_COLOR);
                consumer.consume(quad(matrices, x0, y0, zOffset * zi, w, h, bgc), -1);
            }

            //restore buffer data
            xb.put(0, finalX); yb.put(0, finalY);
        }, Style.EMPTY);
    }

    private void bakeString(VertexConsumer consumer, MatrixStack matrices, String s, boolean italic, boolean bold, boolean obfuscated, boolean underlined, boolean strikethrough, float x, float y, float z, int color) {
        //prepare vars
        RANDOM.setSeed(SEED);
        float preX = xb.get(0);

        //render chars
        for (int i = 0; i < s.length(); ) {
            int c = s.codePointAt(i);
            i += Character.charCount(c);

            if (obfuscated && !Character.isSpaceChar(c))
                c = getRandomCodepoint(c);

            bakeChar(consumer, matrices, c, italic, bold, x, y, z, color);

            if (!obfuscated && i < s.length())
                xb.put(0, xb.get(0) + getKerning(c, s.codePointAt(i)));
        }

        //extra rendering
        float x0 = preX + x, y0 = yb.get(0) + y;
        float width = xb.get(0) - preX + (italic ? ITALIC_OFFSET : 0f);

        //underline
        if (underlined)
            consumer.consume(quad(matrices, x0, y0, z, width, 1f, color), -1);

        //strikethrough
        if (strikethrough)
            consumer.consume(quad(matrices, x0, y0 - (int) (ascent / 2), z, width, 1f, color), -1);
    }

    private void bakeChar(VertexConsumer consumer, MatrixStack matrices, int c, boolean italic, boolean bold, float x, float y, float z, int color) {
        getCharData(c);

        float
                x0 = q.x0(), x1 = q.x1(),
                y0 = q.y0(), y1 = q.y1(),
                u0 = q.s0(), u1 = q.s1(),
                v0 = q.t0(), v1 = q.t1();

        if (x0 == x1 || y0 == y1)
            return;

        float i0 = 0f, i1 = 0f;
        if (italic) {
            i0 = ((y0 + descent) / lineHeight) * -ITALIC_OFFSET;
            i1 = ((y1 + descent) / lineHeight) * -ITALIC_OFFSET;
        }

        x0 += x; x1 += x;
        y0 += y; y1 += y;

        consumer.consume(bakeQuad(matrices, x0, x1, i0, i1, y0, y1, z, u0, u1, v0, v1, color), textureID);

        if (bold) {
            xb.put(0, xb.get(0) + BOLD_OFFSET);
            x0 += BOLD_OFFSET; x1 += BOLD_OFFSET;
            consumer.consume(bakeQuad(matrices, x0, x1, i0, i1, y0, y1, z, u0, u1, v0, v1, color), textureID);
        }
    }

    private static Vertex[] bakeQuad(MatrixStack matrices, float x0, float x1, float i0, float i1, float y0, float y1, float z, float u0, float u1, float v0, float v1, int color) {
        return new Vertex[]{
                Vertex.of(x0 + i1, y1, z).uv(u0, v1).color(color).mul(matrices),
                Vertex.of(x1 + i1, y1, z).uv(u1, v1).color(color).mul(matrices),
                Vertex.of(x1 + i0, y0, z).uv(u1, v0).color(color).mul(matrices),
                Vertex.of(x0 + i0, y0, z).uv(u0, v0).color(color).mul(matrices),
        };
    }


    // -- misc functions -- //


    public int getRandomCodepoint(int codepoint) {
        List<Integer> list = charsByWidth.get(width(codepoint));
        return list == null ? codepoint : list.get(RANDOM.nextInt(list.size()));
    }

    public float getKerning(int codepoint, int next) {
        return stbtt_GetCodepointKernAdvance(info, codepoint, next) * scale;
    }

    public boolean isMissingGlyph(int codepoint) {
        getCharData(codepoint);
        return q.s0() == missing[0] && q.s1() == missing[1] && q.t0() == missing[2] && q.t1() == missing[3];
    }

    public float width(int codepoint) {
        return charData.get(codepoint).xadvance();
    }

    public float width(String string) {
        float w = 0;

        for (int i = 0; i < string.length(); ) {
            int c = string.codePointAt(i);
            i += Character.charCount(c);
            w += width(c);

            if (i < string.length())
                w += getKerning(c, string.codePointAt(i));
        }

        return w;
    }

    public float width(Text text) {
        //prepare vars
        boolean[] prevItalic = {false};
        float[] x = {0f};

        //iterate text
        text.visit((s, style) -> {
            int length = s.length();
            boolean bold = style.isBold();
            boolean italic = style.isItalic();

            //bold special
            if (bold)
                x[0] += BOLD_OFFSET * length;

            //italic special
            if (prevItalic[0] && !italic)
                x[0] += ITALIC_OFFSET;
            prevItalic[0] = italic;

            //add string width
            x[0] += width(s);
        }, Style.EMPTY);

        return x[0];
    }

    public Text clampToWidth(Text text, int width) {
        return clampToWidth(text, width, false);
    }

    public Text clampToWidth(Text text, int width, boolean roundToClosest) {
        //prepare vars
        Text builder = Text.empty();
        boolean[] prevItalic = {false};
        float[] x = {0f, 0f};

        //iterate text
        text.visit((s, style) -> {
            boolean bold = style.isBold();
            boolean italic = style.isItalic();

            //italic
            if (!prevItalic[0] && italic)
                x[0] += ITALIC_OFFSET;
            prevItalic[0] = italic;

            //text allowed to add
            StringBuilder current = new StringBuilder();
            boolean stop = false;

            //iterate over the text
            for (int i = 0; i < s.length(); ) {
                //char
                int c = s.codePointAt(i);
                i += Character.charCount(c);
                x[0] += width(c);

                //kerning
                if (i < s.length())
                    x[0] += getKerning(c, s.codePointAt(i));

                //bold special
                if (bold)
                    x[0] += BOLD_OFFSET;

                //check width
                if (x[0] <= width) {
                    current.appendCodePoint(c);
                } else {
                    if (roundToClosest && x[0] - width < width - x[1])
                        current.appendCodePoint(c);
                    stop = true;
                    break;
                }

                x[1] = x[0];
            }

            //append allowed text
            builder.append(Text.of(current.toString()).withStyle(style));
            return stop;
        }, Style.EMPTY);

        //return
        return builder;
    }
}
