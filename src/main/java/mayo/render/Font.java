package mayo.render;

import mayo.model.Renderable;
import mayo.model.Vertex;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

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

    public static final Random RANDOM = new Random();

    //buffers
    private final STBTTAlignedQuad q = STBTTAlignedQuad.malloc();
    private final FloatBuffer xb = memAllocFloat(2), yb = memAllocFloat(2);

    //fields
    private final ByteBuffer ttf;
    private final STBTTPackedchar.Buffer charData;
    private final int textureID;
    public final float lineHeight;

    public Font(String namespace, String name, float height) {
        this.ttf = IOUtils.getResourceBuffer(namespace, "fonts/" + name + ".ttf");
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
    }

    public void free() {
        charData.free();
    }

    public Renderable bake(Text text) {
        List<Vertex> vertices = bake(text, text.getStyle(), 0f, lineHeight - 1f);
        vertices.sort(Vertex::compareTo);
        return new Renderable(vertices.toArray(new Vertex[0]), textureID);
    }

    private List<Vertex> bake(Text text, Style style, float x, float y) {
        //prepare vars
        int seed = RANDOM.nextInt();
        String str = text.getText();
        boolean bold = style.isBold();
        boolean italic = style.isItalic();

        //render first layer
        RANDOM.setSeed(seed);
        int color = Objects.requireNonNullElse(style.getColor(), -1);
        List<Vertex> vertices = new ArrayList<>(bakeString(str, bold, italic, color, x, y, 0f, 0f, 0));

        //render shadow
        if (style.hasShadow()) {
            RANDOM.setSeed(seed);
            int shadowColor = Objects.requireNonNullElse(style.getShadowColor(), SHADOW_COLOR);
            vertices.addAll(bakeString(str, bold, italic, shadowColor, x, y, SHADOW_OFFSET, SHADOW_OFFSET, -1));
        }

        //render outline
        if (style.hasOutline()) {
            int outlineColor = Objects.requireNonNullElse(style.getOutlineColor(), SHADOW_COLOR);
            for (int i = -SHADOW_OFFSET; i <= SHADOW_OFFSET; i += SHADOW_OFFSET) {
                for (int j = -SHADOW_OFFSET; j <= SHADOW_OFFSET; j += SHADOW_OFFSET) {
                    if (i != 0 || j != 0) {
                        RANDOM.setSeed(seed);
                        vertices.addAll(bakeString(str, bold, italic, outlineColor, x, y, i, j, -2));
                    }
                }
            }
        }

        //children
        for (Text child : text.getChildren()) {
            Style next = child.getStyle().applyParent(style);
            float italicOffset = italic && !next.isItalic() ? ITALIC_OFFSET : 0f;
            vertices.addAll(bake(child, next, xb.get(1) + xb.get(0) + italicOffset, yb.get(1)));
        }

        return vertices;
    }

    private List<Vertex> bakeString(String text, boolean bold, boolean italic, int color, float x, float y, float xOff, float yOff, int level) {
        //prepare buffers
        xb.put(0, xOff); xb.put(1, x);
        yb.put(0, yOff); yb.put(1, y);

        List<Vertex> vertices = new ArrayList<>();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                xb.put(0, xOff);
                yb.put(0, yOff);
                yb.put(1, yb.get(1) + lineHeight);
                continue;
            }
            vertices.addAll(bakeChar(c, bold, italic, color, xb.get(1), yb.get(1), level));
        }

        xb.put(0, xb.get(0) - xOff);
        yb.put(0, yb.get(0) - yOff);
        return vertices;
    }

    private List<Vertex> bakeChar(char c, boolean bold, boolean italic, int color, float x, float y, int level) {
        stbtt_GetPackedQuad(charData, TEXTURE_W, TEXTURE_H, c, xb, yb, q, false);

        float
                x0 = q.x0(), x1 = q.x1(),
                y0 = q.y0(), y1 = q.y1(),
                u0 = q.s0(), u1 = q.s1(),
                v0 = q.t0(), v1 = q.t1();

        float i0 = italic ? ((y0 - 1) / lineHeight) * -ITALIC_OFFSET : 0f;
        float i1 = italic ? ((y1 - 1) / lineHeight) * -ITALIC_OFFSET : 0f;

        x0 += x; x1 += x;
        y0 += y; y1 += y;

        if (bold) {
            xb.put(0, xb.get(0) + BOLD_OFFSET);

            ArrayList<Vertex> list = new ArrayList<>(bakeQuad(x0, x1, i0, i1, y0, y1, u0, u1, v0, v1, color, level));
            x0 += BOLD_OFFSET; x1 += BOLD_OFFSET;
            list.addAll(bakeQuad(x0, x1, i0, i1, y0, y1, u0, u1, v0, v1, color, level));

            return list;
        } else {
            return bakeQuad(x0, x1, i0, i1, y0, y1, u0, u1, v0, v1, color, level);
        }
    }

    private List<Vertex> bakeQuad(float x0, float x1, float i0, float i1, float y0, float y1, float u0, float u1, float v0, float v1, int color, int level) {
        return List.of(
                Vertex.of(x0 + i0, y0, 0f).uv(u0, v0).color(color).index(level),
                Vertex.of(x1 + i0, y0, 0f).uv(u1, v0).color(color).index(level),
                Vertex.of(x1 + i1, y1, 0f).uv(u1, v1).color(color).index(level),
                Vertex.of(x0 + i1, y1, 0f).uv(u0, v1).color(color).index(level)
        );
    }

    public float getWidth(Text text) {
        return 0;
    }

    public float getHeight(Text text) {
        String[] split = text.asString().split("\n", -1);
        return lineHeight * split.length;
    }
}
