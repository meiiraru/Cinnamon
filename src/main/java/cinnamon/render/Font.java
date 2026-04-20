package cinnamon.render;

import cinnamon.Client;
import cinnamon.model.Vertex;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static cinnamon.events.Events.LOGGER;
import static cinnamon.model.GeometryHelper.rectangle;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL33.GL_TEXTURE_SWIZZLE_RGBA;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Font {

    //cache
    private static final Map<Resource, Font> FONTS_CACHE = new HashMap<>();

    //properties
    public static final int TEXTURE_W = 512, TEXTURE_H = 512;
    public static final int UNICODE_PAGE_SIZE = 0x100;
    public static final int UNICODE_MAX_CODEPOINT = 0x10FFFF;
    public static final int EXTRA_GLYPH_SLOTS = 3; // ".notdef" ".null" "nonmarkingreturn"
    public static final int MISSING_GLYPH_CODEPOINT = UNICODE_MAX_CODEPOINT + 1;
    public static final float Z_DEPTH = 3;

    private static final Random RANDOM = new Random();
    private static long SEED = 42;

    //buffers
    private final STBTTAlignedQuad q = STBTTAlignedQuad.malloc();
    private final FloatBuffer xb = memAllocFloat(1), yb = memAllocFloat(1);

    //data
    private final ByteBuffer ttf; //needs to be kept in memory
    private final STBTTFontinfo info = STBTTFontinfo.malloc();
    private final Map<Integer, STBTTPackedchar.Buffer> glyphPages = new HashMap<>();
    private final STBTTPackedchar.Buffer missingCharData;
    private final int textureID;
    private Font fallback;

    //properties
    public final float
            lineHeight,
            lineGap,
            ascent,
            descent,
            scale;

    private final Map<Float, List<Integer>> charsByWidth = new HashMap<>();


    // -- font initialization -- //


    public static Font getFont(Resource res, float height) {
        return getFont(res, height, 1, false);
    }

    public static Font getFont(Resource res, float height, float lineSpacing, boolean smooth) {
        return FONTS_CACHE.computeIfAbsent(res, r -> new Font(r, height, lineSpacing, smooth));
    }

    private Font(Resource res, float height, float lineSpacing, boolean smooth) {
        ByteBuffer buffer = IOUtils.getResourceBuffer(res);
        this.ttf = memAlloc(buffer.capacity()).put(buffer).flip();
        this.lineHeight = height;
        this.lineGap = lineSpacing;
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

            //pack missing char data first
            this.missingCharData = STBTTPackedchar.malloc(EXTRA_GLYPH_SLOTS);
            stbtt_PackFontRange(spc, this.ttf, 0, height, MISSING_GLYPH_CODEPOINT, this.missingCharData);

            //discover supported ranges and pack them
            for (int pageStart = 0; pageStart <= UNICODE_MAX_CODEPOINT; pageStart += UNICODE_PAGE_SIZE) {
                //check if the page has a glyph
                int pageEnd = pageStart + UNICODE_PAGE_SIZE - 1;
                for (int codepoint = pageStart; codepoint <= pageEnd; codepoint++) {
                    //if so, add the page to the pages list
                    if (hasGlyph(codepoint)) {
                        STBTTPackedchar.Buffer rangeData = STBTTPackedchar.malloc(UNICODE_PAGE_SIZE);
                        stbtt_PackFontRange(spc, this.ttf, 0, height, pageStart, rangeData);
                        this.glyphPages.put(pageStart, rangeData);
                        break;
                    }
                }
            }

            stbtt_PackEnd(spc);

            glBindTexture(GL_TEXTURE_2D, textureID);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, TEXTURE_W, TEXTURE_H, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, smooth ? GL_LINEAR : GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, smooth ? GL_LINEAR : GL_NEAREST);

            int[] swizzleMask = {GL_RED, GL_RED, GL_RED, GL_RED};
            glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_RGBA, swizzleMask);

            glBindTexture(GL_TEXTURE_2D, 0);
        }

        //char widths
        for (int start : glyphPages.keySet()) {
            int end = start + UNICODE_PAGE_SIZE - 1;
            for (int i = start; i <= end; i++) {
                if (Character.isSpaceChar(i) || !hasGlyph(i))
                    continue;
                float w = width(i);
                charsByWidth.computeIfAbsent(w, k -> new ArrayList<>()).add(i);
            }
        }

        //finished!
        LOGGER.debug("Loaded font \"%s\"", res);
    }

    public static void freeAll() {
        for (Font font : FONTS_CACHE.values())
            font.free();
        FONTS_CACHE.clear();
    }

    public void setFallback(Font fallback) {
        this.fallback = fallback == this ? null : fallback;
    }

    public void free() {
        glDeleteTextures(textureID);
        for (STBTTPackedchar.Buffer buffer : glyphPages.values())
            buffer.free();
        glyphPages.clear();
        missingCharData.free();
        info.free();
        q.free();
        memFree(xb);
        memFree(yb);
        memFree(ttf);
    }

    private int getCodepointPage(int codepoint) {
        return codepoint - (codepoint % UNICODE_PAGE_SIZE);
    }

    private int getCharData(int c) {
        //get the font that has the glyph
        Font owner = findGlyphOwner(c);
        if (owner != null) {
            //get the char data from the owner font
            int page = owner.getCodepointPage(c);
            STBTTPackedchar.Buffer buffer = owner.glyphPages.get(page);
            if (buffer != null) {
                //if the owner has the page, get the char data
                stbtt_GetPackedQuad(buffer, TEXTURE_W, TEXTURE_H, c - page, xb, yb, q, false);
                return owner.textureID;
            }
        }

        //no owner, use missing char data
        stbtt_GetPackedQuad(missingCharData, TEXTURE_W, TEXTURE_H, 0, xb, yb, q, false);
        return textureID;
    }

    private boolean hasGlyph(int codepoint) {
        return codepoint >= 0 && codepoint <= UNICODE_MAX_CODEPOINT && stbtt_FindGlyphIndex(info, codepoint) != 0;
    }

    private Font findGlyphOwner(int codepoint) {
        Set<Font> visited = new HashSet<>();
        Font current = this;

        while (current != null && visited.add(current)) {
            if (current.hasGlyph(codepoint))
                return current;
            current = current.fallback;
        }

        return null;
    }


    // -- font rendering -- //


    public void bake(VertexConsumer consumer, MatrixStack matrices, Text text, float x, float y, float zOffset) {
        //prepare vars
        boolean[] prevItalic = {false};
        xb.put(0, 0f); yb.put(0, 0f);
        SEED = Client.getInstance().ticks;

        //iterate text and children
        text.visit((s, style) -> {
            if (s.isEmpty())
                return;

            //style flags
            boolean italic   = style.isItalic();
            boolean bold     = style.isBold();
            boolean obf      = style.isObfuscated();
            boolean shadow   = style.hasShadow();
            boolean outline  = style.hasOutline();
            boolean bg       = style.hasBackground();
            boolean under    = style.isUnderlined();
            boolean strike   = style.isStrikethrough();
            int italicOffset = style.getItalicOffset();
            int boldOffset   = style.getBoldOffset();
            int shadowOffset = style.getShadowOffset();

            int zi = (shadow ? 1 : 0) + (outline ? 1 : 0) + (bg ? 1 : 0);

            if (prevItalic[0] && !italic)
                xb.put(0, xb.get(0) + italicOffset);
            prevItalic[0] = italic;

            //backup initial buffer
            float initialX = xb.get(0), initialY = yb.get(0);

            //main render
            int color = style.getColor();
            bakeString(consumer, matrices, s, italic, bold, obf, under, strike, x, y, zOffset * zi--, color, italicOffset, boldOffset);

            //backup final buffer
            float finalX = xb.get(0), finalY = yb.get(0);

            //render shadow
            if (shadow) {
                xb.put(0, initialX + shadowOffset); yb.put(0, initialY + shadowOffset);
                int sc = style.getShadowColor();
                bakeString(consumer, matrices, s, italic, bold, obf, under, strike, x, y, zOffset * zi--, sc, italicOffset, boldOffset);
            }

            //render outline
            if (outline) {
                int oc = style.getOutlineColor();
                for (int i = -shadowOffset; i <= shadowOffset; i += shadowOffset) {
                    for (int j = -shadowOffset; j <= shadowOffset; j += shadowOffset) {
                        xb.put(0, initialX + i); yb.put(0, initialY + j);
                        bakeString(consumer, matrices, s, italic, bold, obf, under, strike, x, y, zOffset * zi, oc, italicOffset, boldOffset);
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
                    w += italicOffset;

                if (outline) {
                    x0--; y0--;
                    w += 2; h += 2;
                } else if (shadow) {
                    w++; h++;
                }

                int bgc = style.getBackgroundColor();
                consumer.consume(rectangle(matrices, x0, y0, x0 + w, y0 + h, zOffset * zi, bgc));
            }

            //restore buffer data
            xb.put(0, finalX); yb.put(0, finalY);
        }, Style.EMPTY);
    }

    private void bakeString(VertexConsumer consumer, MatrixStack matrices, String s, boolean italic, boolean bold, boolean obfuscated, boolean underlined, boolean strikethrough, float x, float y, float z, int color, int italicOffset, int boldOffset) {
        //prepare vars
        RANDOM.setSeed(SEED);
        float preX = xb.get(0);

        //render chars
        for (int i = 0; i < s.length(); ) {
            int c = s.codePointAt(i);
            i += Character.charCount(c);

            if (obfuscated && !Character.isSpaceChar(c))
                c = getRandomCodepoint(c);

            bakeChar(consumer, matrices, c, italic, bold, x, y, z, color, italicOffset, boldOffset);

            if (!obfuscated && i < s.length())
                xb.put(0, xb.get(0) + getKerning(c, s.codePointAt(i)));
        }

        //extra rendering
        float x0 = preX + x, y0 = yb.get(0) + y;
        float width = xb.get(0) - preX + (italic ? italicOffset : 0f);

        //underline
        if (underlined)
            consumer.consume(rectangle(matrices, x0, y0, x0 + width, y0 + 1, z, color));

        //strikethrough
        if (strikethrough) {
            float rectY = y0 - (int) (ascent / 2);
            consumer.consume(rectangle(matrices, x0, rectY, x0 + width, rectY + 1f, color));
        }
    }

    private void bakeChar(VertexConsumer consumer, MatrixStack matrices, int c, boolean italic, boolean bold, float x, float y, float z, int color, int italicOffset, int boldOffset) {
        int glyphTexture = getCharData(c);

        float
                x0 = q.x0(), x1 = q.x1(),
                y0 = q.y0(), y1 = q.y1(),
                u0 = q.s0(), u1 = q.s1(),
                v0 = q.t0(), v1 = q.t1();

        if (x0 == x1 || y0 == y1)
            return;

        float i0 = 0f, i1 = 0f;
        if (italic) {
            i0 = ((y0 + descent) / lineHeight) * -italicOffset;
            i1 = ((y1 + descent) / lineHeight) * -italicOffset;
        }

        x0 += x; x1 += x;
        y0 += y; y1 += y;

        consumer.consume(bakeQuad(matrices, x0, x1, i0, i1, y0, y1, z, u0, u1, v0, v1, color), glyphTexture);

        if (bold) {
            xb.put(0, xb.get(0) + boldOffset);
            x0 += boldOffset; x1 += boldOffset;
            consumer.consume(bakeQuad(matrices, x0, x1, i0, i1, y0, y1, z, u0, u1, v0, v1, color), glyphTexture);
        }
    }

    private static Vertex[] bakeQuad(MatrixStack matrices, float x0, float x1, float i0, float i1, float y0, float y1, float z, float u0, float u1, float v0, float v1, int color) {
        return new Vertex[]{
                new Vertex().pos(x0 + i1, y1, z).uv(u0, v1).color(color).mul(matrices),
                new Vertex().pos(x1 + i1, y1, z).uv(u1, v1).color(color).mul(matrices),
                new Vertex().pos(x1 + i0, y0, z).uv(u1, v0).color(color).mul(matrices),
                new Vertex().pos(x0 + i0, y0, z).uv(u0, v0).color(color).mul(matrices),
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

    public float width(int codepoint) {
        //get the font that has the glyph
        Font owner = findGlyphOwner(codepoint);
        if (owner != null) {
            //get the char data from the owner font
            int page = owner.getCodepointPage(codepoint);
            STBTTPackedchar.Buffer buffer = owner.glyphPages.get(page);
            if (buffer != null)
                return buffer.get(codepoint - page).xadvance();
        }

        //no owner, use missing char data
        return missingCharData.get(0).xadvance();
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
            //italic
            boolean italic = style.isItalic();
            if (prevItalic[0] && !italic)
                x[0] += style.getItalicOffset();
            prevItalic[0] = italic;

            //bold
            if (style.isBold())
                x[0] += style.getBoldOffset() * s.codePoints().count();

            //add string width
            x[0] += width(s);
        }, Style.EMPTY);

        return x[0];
    }
}
