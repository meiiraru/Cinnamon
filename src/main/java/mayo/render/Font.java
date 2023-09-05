package mayo.render;

import mayo.model.Renderable;
import mayo.model.Vertex;
import mayo.utils.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Font {

    private static final int TEXTURE_W = 2048, TEXTURE_H = 2048;

    private static final float SHADOW_OFFSET = 0.001f;
    private static final int SHADOW_COLOR = 0x202020;

    private final ByteBuffer fontData;
    private final STBTTFontinfo info;
    private final int ascent, descent, lineGap;
    private final float scale;
    private final STBTTBakedChar.Buffer charData;
    private final int textureID;
    public final float lineHeight;

    public Font(String namespace, String name, float height) {
        //get font
        this.fontData = IOUtils.getResourceBuffer(namespace, "fonts/" + name + ".ttf");

        //load font
        this.info = STBTTFontinfo.create();
        if (!stbtt_InitFont(info, fontData))
            throw new RuntimeException("Failed to initialize font \"" + name + "\"");

        this.lineHeight = height;
        this.scale = stbtt_ScaleForPixelHeight(info, lineHeight);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            //read font data
            IntBuffer pAscent = stack.mallocInt(1);
            IntBuffer pDescent = stack.mallocInt(1);
            IntBuffer pLineGap = stack.mallocInt(1);

            stbtt_GetFontVMetrics(info, pAscent, pDescent, pLineGap);

            this.ascent = pAscent.get(0);
            this.descent = pDescent.get(0);
            this.lineGap = pLineGap.get(0);
        }

        this.charData = STBTTBakedChar.malloc(0xFFFF);
        this.textureID = generateBitmap();
    }

    private int generateBitmap() {
        int id = glGenTextures();
        ByteBuffer bitmap = BufferUtils.createByteBuffer(TEXTURE_W * TEXTURE_H);
        stbtt_BakeFontBitmap(fontData, lineHeight, bitmap, TEXTURE_W, TEXTURE_H, 0, charData);

        glBindTexture(GL_TEXTURE_2D, id);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, TEXTURE_W, TEXTURE_H, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);

        return id;
    }

    public void free() {
        charData.free();
    }

    public Renderable textOf(String text) {
        return textOf(text, -1);
    }

    public Renderable textOf(String text, int color) {
        return textOf(text, color, false, false);
    }

    public Renderable textOf(String text, int color, boolean shadow, boolean outline) {
        List<Vertex> vertices = new ArrayList<>();
        float z = shadow || outline ? SHADOW_OFFSET : 0f;

        try (MemoryStack stack = stackPush()) {
            IntBuffer pCodePoint = stack.mallocInt(1);

            FloatBuffer x = stack.floats(0f);
            FloatBuffer y = stack.floats(lineHeight - 1);

            STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);

            for (int curIndex = 0, endIndex = text.length(); curIndex < endIndex;) {
                curIndex += getCP(text, endIndex, curIndex, pCodePoint);

                int cp = pCodePoint.get(0);
                if (cp == '\n') {
                    y.put(0, y.get(0) + (ascent - descent + lineGap) * scale);
                    x.put(0, 0f);
                    continue;
                }

                stbtt_GetBakedQuad(charData, TEXTURE_W, TEXTURE_H, cp, x, y, q, true);
                x.put(0, x.get(0));

                float
                        x0 = q.x0(), x1 = q.x1(),
                        y0 = q.y0(), y1 = q.y1(),
                        u0 = q.s0(), u1 = q.s1(),
                        v0 = q.t0(), v1 = q.t1();

                if (shadow) {
                    vertices.addAll(bakeQuad(x0 + 1, x1 + 1, y0 + 1, y1 + 1, 0f, u0, u1, v0, v1, SHADOW_COLOR));
                } else if (outline) {
                    for (int i = -1; i <= 1; i++) {
                        for (int j = -1; j <= 1; j++) {
                            if (i != 0 || j != 0)
                                vertices.addAll(bakeQuad(x0 + i, x1 + i, y0 + j, y1 + j, 0f, u0, u1, v0, v1, SHADOW_COLOR));
                        }
                    }
                }

                vertices.addAll(bakeQuad(x0, x1, y0, y1, z, u0, u1, v0, v1, color));
            }
        }

        return new Renderable(vertices.toArray(new Vertex[0]), textureID);
    }

    private static int getCP(String text, int endIndex, int curIndex, IntBuffer cpOut) {
        char c1 = text.charAt(curIndex);
        if (Character.isHighSurrogate(c1) && curIndex + 1 < endIndex) {
            char c2 = text.charAt(curIndex + 1);
            if (Character.isLowSurrogate(c2)) {
                cpOut.put(0, Character.toCodePoint(c1, c2));
                return 2;
            }
        }
        cpOut.put(0, c1);
        return 1;
    }

    private static List<Vertex> bakeQuad(float x0, float x1, float y0, float y1, float z, float u0, float u1, float v0, float v1, int color) {
        return List.of(
                new Vertex(x0, y0, z, u0, v0, color),
                new Vertex(x1, y0, z, u1, v0, color),
                new Vertex(x1, y1, z, u1, v1, color),
                new Vertex(x0, y1, z, u0, v1, color)
        );
    }

    public float getWidth(String text) {
        int width = 0, curWidth = 0;

        try (MemoryStack stack = stackPush()) {
            IntBuffer pCodePoint = stack.mallocInt(1);
            IntBuffer pAdvancedWidth = stack.mallocInt(1);
            IntBuffer pLeftSideBearing = stack.mallocInt(1);

            int curIndex = 0, endIndex = text.length();
            while (curIndex < endIndex) {
                curIndex += getCP(text, endIndex, curIndex, pCodePoint);
                int cp = pCodePoint.get(0);

                if (cp == '\n') {
                    width = Math.max(width, curWidth);
                    curWidth = 0;
                    continue;
                }

                stbtt_GetCodepointHMetrics(info, cp, pAdvancedWidth, pLeftSideBearing);
                curWidth += pAdvancedWidth.get(0);
            }
        }

        return Math.max(width, curWidth) * scale;
    }

    public float getHeight(String text) {
        String[] split = text.split("\n", -1);
        return lineHeight * split.length;
    }
}
