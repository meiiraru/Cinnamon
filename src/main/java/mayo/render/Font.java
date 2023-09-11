package mayo.render;

import mayo.model.Renderable;
import mayo.model.Vertex;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.IOUtils;
import mayo.utils.Resource;
import mayo.utils.TextUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;

import static mayo.model.GeometryHelper.quad;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;

public class Font {

    //texture
    private static final int TEXTURE_W = 512, TEXTURE_H = 512;

    //properties
    private static final int SHADOW_COLOR = 0x202020;
    private static final int SHADOW_OFFSET = 1;
    private static final int BOLD_OFFSET = 1;
    private static final int ITALIC_OFFSET = 3;
    private static final float Z_OFFSET = 0.001f;

    public static final Random RANDOM = new Random();
    private static int SEED = 42;

    //buffers
    private final STBTTAlignedQuad q = STBTTAlignedQuad.malloc();
    private final FloatBuffer xb = memAllocFloat(1), yb = memAllocFloat(1);

    //fields
    private final ByteBuffer ttf;
    private final STBTTPackedchar.Buffer charData;
    private final int textureID;
    public final float lineHeight;

    //other
    private final Map<Float, List<Character>> charsByWidth = new HashMap<>();

    // -- font initialization -- //

    public Font(Resource res, float height) {
        this.ttf = IOUtils.getResourceBuffer(res);
        this.lineHeight = height;
        this.charData = STBTTPackedchar.malloc(0xFFFF);
        this.textureID = glGenTextures();

        try (STBTTPackContext spc = STBTTPackContext.malloc()) {
            ByteBuffer bitmap = BufferUtils.createByteBuffer(TEXTURE_W * TEXTURE_H);

            stbtt_PackSetSkipMissingCodepoints(spc, true);
            stbtt_PackBegin(spc, bitmap, TEXTURE_W, TEXTURE_H, 0, 1, NULL);
            stbtt_PackFontRange(spc, ttf, 0, height, 0, charData);

            charData.clear();
            stbtt_PackEnd(spc);

            glBindTexture(GL_TEXTURE_2D, textureID);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, TEXTURE_W, TEXTURE_H, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);
        }

        for (int i = 0; i < 0xFFFF; i++) {
            char c = (char) i;
            float w = getCharWidth(c);
            charsByWidth.computeIfAbsent(w, k -> new ArrayList<>()).add(c);
        }
    }

    public void free() {
        charData.free();
    }

    // -- font rendering -- //

    public Renderable bake(Text text) {
        return bake(text, TextUtils.Alignment.LEFT);
    }

    public Renderable bake(Text text, TextUtils.Alignment alignment) {
        List<Text> list = TextUtils.split(text, "\n");
        List<Vertex> vertices = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            Text t = list.get(i);
            int x = (int) alignment.apply(this, t);
            int y = (int) (lineHeight * (i + 1) - 1);
            bake(vertices, t, x, y);
        }

        vertices.sort(Vertex::compareTo);
        return new Renderable(vertices.toArray(new Vertex[0]), textureID);
    }

    private void bake(List<Vertex> list, Text text, float x, float y) {
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

            if (prevItalic[0] && !italic)
                xb.put(0, xb.get(0) + ITALIC_OFFSET);
            prevItalic[0] = italic;

            //backup initial buffer
            float initialX = xb.get(0), initialY = yb.get(0);

            //main render
            int color = Objects.requireNonNullElse(style.getColor(), -1);
            bakeString(list, s, italic, bold, obf, under, strike, x, y, color, 0);

            //backup final buffer
            float finalX = xb.get(0), finalY = yb.get(0);

            //render shadow
            if (shadow) {
                xb.put(0, initialX + SHADOW_OFFSET); yb.put(0, initialY + SHADOW_OFFSET);
                int sc = Objects.requireNonNullElse(style.getShadowColor(), SHADOW_COLOR);
                bakeString(list, s, italic, bold, obf, under, strike, x, y, sc, -1);
            }

            //render outline
            if (outline) {
                int oc = Objects.requireNonNullElse(style.getOutlineColor(), SHADOW_COLOR);
                for (int i = -SHADOW_OFFSET; i <= SHADOW_OFFSET; i += SHADOW_OFFSET) {
                    for (int j = -SHADOW_OFFSET; j <= SHADOW_OFFSET; j += SHADOW_OFFSET) {
                        xb.put(0, initialX + i); yb.put(0, initialY + j);
                        bakeString(list, s, italic, bold, obf, under, strike, x, y, oc, -2);
                    }
                }
            }

            //render background
            if (bg) {
                float x0 = initialX + x;
                float y0 = initialY + y - lineHeight + 1;
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

                int bgc = Objects.requireNonNullElse(style.getBackgroundColor(), SHADOW_COLOR);
                list.addAll(quad(x0, y0, Z_OFFSET * -3, w, h, bgc, -3));
            }

            //restore buffer data
            xb.put(0, finalX); yb.put(0, finalY);
        }, Style.EMPTY);
    }

    private void bakeString(List<Vertex> list, String s, boolean italic, boolean bold, boolean obfuscated, boolean underlined, boolean strikethrough, float x, float y, int color, int level) {
        //prepare vars
        RANDOM.setSeed(SEED);
        float preX = xb.get(0);
        float z = Z_OFFSET * level;

        //render chars
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (obfuscated && c != ' ') //32
                c = getRandomChar(c);
            bakeChar(list, c, italic, bold, x, y, z, color, level);
        }

        //extra rendering
        float x0 = preX + x, y0 = yb.get(0) + y;
        float width = xb.get(0) - preX + (italic ? ITALIC_OFFSET : 0f);

        //underline
        if (underlined)
            list.addAll(quad(x0, y0, z, width, 1f, color, level));

        //strikethrough
        if (strikethrough)
            list.addAll(quad(x0, y0 - (int) (lineHeight / 2 - 1), z, width, 1f, color, level));
    }

    private void bakeChar(List<Vertex> list, char c, boolean italic, boolean bold, float x, float y, float z, int color, int level) {
        stbtt_GetPackedQuad(charData, TEXTURE_W, TEXTURE_H, c, xb, yb, q, false);

        float
                x0 = q.x0(), x1 = q.x1(),
                y0 = q.y0(), y1 = q.y1(),
                u0 = q.s0(), u1 = q.s1(),
                v0 = q.t0(), v1 = q.t1();

        float i0 = 0f, i1 = 0f;
        if (italic) {
            i0 = ((y0 - 1) / lineHeight) * -ITALIC_OFFSET;
            i1 = ((y1 - 1) / lineHeight) * -ITALIC_OFFSET;
        }

        x0 += x; x1 += x;
        y0 += y; y1 += y;

        bakeQuad(list, x0, x1, i0, i1, y0, y1, z, u0, u1, v0, v1, color, level);

        if (bold) {
            xb.put(0, xb.get(0) + BOLD_OFFSET);
            x0 += BOLD_OFFSET; x1 += BOLD_OFFSET;
            bakeQuad(list, x0, x1, i0, i1, y0, y1, z, u0, u1, v0, v1, color, level);
        }
    }

    private static void bakeQuad(List<Vertex> list, float x0, float x1, float i0, float i1, float y0, float y1, float z, float u0, float u1, float v0, float v1, int color, int level) {
        list.add(Vertex.of(x0 + i0, y0, z).uv(u0, v0).color(color).index(level));
        list.add(Vertex.of(x1 + i0, y0, z).uv(u1, v0).color(color).index(level));
        list.add(Vertex.of(x1 + i1, y1, z).uv(u1, v1).color(color).index(level));
        list.add(Vertex.of(x0 + i1, y1, z).uv(u0, v1).color(color).index(level));
    }

    private char getRandomChar(char c) {
        List<Character> list = charsByWidth.get(getCharWidth(c));
        return list == null ? c : list.get(RANDOM.nextInt(list.size()));
    }

    // -- misc functions -- //

    private float getCharWidth(char c) {
        //backup buffer
        float x = xb.get(0), y = yb.get(0);

        //fill data
        stbtt_GetPackedQuad(charData, TEXTURE_W, TEXTURE_H, c, xb, yb, q, false);

        //save return value
        float w = xb.get(0) - x;

        //restore buffer
        xb.put(0, x); yb.put(0, y);

        return w;
    }

    public float width(Text text) {
        //prepare vars
        boolean[] prevItalic = {false};
        xb.put(0, 0f); yb.put(0, 0f);

        //iterate text
        text.visit((s, style) -> {
            for (int i = 0; i < s.length(); i++) {
                //this function will fill the buffers with all necessary data, as we only want the xb (width)
                stbtt_GetPackedQuad(charData, TEXTURE_W, TEXTURE_H, s.charAt(i), xb, yb, q, false);

                //bold special
                if (style.isBold())
                    xb.put(0, xb.get(0) + BOLD_OFFSET);

                //italic special
                boolean italic = style.isItalic();
                if (prevItalic[0] && !italic)
                    xb.put(0, xb.get(0) + ITALIC_OFFSET);
                prevItalic[0] = italic;
            }
        }, Style.EMPTY);

        return xb.get(0);
    }

    public float height(Text text) {
        String[] split = text.asString().split("\n", -1);
        return lineHeight * split.length;
    }
}
