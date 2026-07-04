package cinnamon.render;

import cinnamon.Client;
import cinnamon.model.Vertex;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
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
    public static final int MIN_SIZE = 16, MAX_SIZE = 2048;
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
    private final Map<Integer, GlyphPage> glyphPages = new HashMap<>();
    private final GlyphPage missingCharPage;
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
        return getFont(res, height, 1, false, 1);
    }

    public static Font getFont(Resource res, float height, float lineSpacing, boolean smooth, int oversample) {
        return FONTS_CACHE.computeIfAbsent(res, r -> new Font(r, height, lineSpacing, smooth, oversample));
    }

    private Font(Resource res, float height, float lineSpacing, boolean smooth, int oversample) {
        ByteBuffer buffer = IOUtils.getResourceBuffer(res);
        this.ttf = memAlloc(buffer.capacity()).put(buffer).flip();
        this.lineHeight = height;
        this.lineGap = lineSpacing;

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

        //pack missing char data first
        this.missingCharPage = packGlyphPage(res, MISSING_GLYPH_CODEPOINT, EXTRA_GLYPH_SLOTS, height, smooth, oversample);

        //discover supported ranges and pack them
        for (int pageStart = 0; pageStart <= UNICODE_MAX_CODEPOINT; pageStart += UNICODE_PAGE_SIZE) {
            //check if the page has a glyph
            int pageEnd = pageStart + UNICODE_PAGE_SIZE - 1;
            boolean hasGlyphs = false;

            for (int codepoint = pageStart; codepoint <= pageEnd; codepoint++) {
                if (hasGlyph(codepoint)) {
                    hasGlyphs = true;
                    break;
                }
            }

            //if so, pack the entire page into its own texture
            if (hasGlyphs) {
                GlyphPage page = packGlyphPage(res, pageStart, UNICODE_PAGE_SIZE, height, smooth, oversample);
                if (page != null)
                    this.glyphPages.put(pageStart, page);
            }
        }

        //calculate char widths
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

    private GlyphPage packGlyphPage(Resource res, int startCodepoint, int numChars, float height, boolean smooth, int oversample) {
        STBTTPackedchar.Buffer charData = STBTTPackedchar.malloc(numChars);

        int texSize = MIN_SIZE;
        while (texSize <= MAX_SIZE) {
            //font bitmap
            try (STBTTPackContext spc = STBTTPackContext.malloc()) {
                ByteBuffer bitmap = memAlloc(texSize * texSize);
                stbtt_PackBegin(spc, bitmap, texSize, texSize, 0, 1, NULL);

                //stbtt_PackSetSkipMissingCodepoints(spc, startCodepoint != MISSING_GLYPH_CODEPOINT);

                //oversampling for better quality at small sizes
                stbtt_PackSetOversampling(spc, oversample, oversample);

                //try to pack the glyphs into the current size
                if (!stbtt_PackFontRange(spc, this.ttf, 0, height, startCodepoint, charData)) {
                    //fail (not enough space)
                    //clean up and try a larger size
                    stbtt_PackEnd(spc);
                    memFree(bitmap);
                    texSize *= 2;
                    continue;
                }

                stbtt_PackEnd(spc);

                //strip blurry edges for non-smooth fonts
                if (!smooth) {
                    for (int i = 0; i < texSize * texSize; i++) {
                        int alpha = bitmap.get(i) & 0xFF;
                        bitmap.put(i, (byte) (alpha > 0 ? 255 : 0));
                    }
                }

                //success! create the texture
                int texID = glGenTextures();
                glBindTexture(GL_TEXTURE_2D, texID);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, texSize, texSize, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);

                int filter = smooth ? GL_LINEAR : GL_NEAREST;
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);

                int[] swizzleMask = {GL_RED, GL_RED, GL_RED, GL_RED};
                glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_RGBA, swizzleMask);

                glBindTexture(GL_TEXTURE_2D, 0);

                //free the RAM buffer as it is now in the GPU VRAM
                memFree(bitmap);
                return new GlyphPage(texID, texSize, texSize, charData);
            }
        }

        //epic fail :(
        LOGGER.warn("Failed to pack glyph page U+%04X..U+%04X for font \"%s\"", startCodepoint, startCodepoint + numChars - 1, res);
        charData.free();
        return null;
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
        for (GlyphPage page : glyphPages.values())
            page.free();
        glyphPages.clear();
        missingCharPage.free();
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
            int pageStart = owner.getCodepointPage(c);
            GlyphPage page = owner.glyphPages.get(pageStart);
            if (page != null) {
                //if the owner has the page, get the char data
                stbtt_GetPackedQuad(page.charData, page.width, page.height, c - pageStart, xb, yb, q, true);
                return page.textureID;
            }
        }

        //no owner, use missing char data
        stbtt_GetPackedQuad(missingCharPage.charData, missingCharPage.width, missingCharPage.height, 0, xb, yb, q, true);
        return missingCharPage.textureID;
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

            if (prevItalic[0] && !italic)
                xb.put(0, xb.get(0) + italicOffset);
            prevItalic[0] = italic;

            //backup initial buffer
            float initialX = xb.get(0), initialY = yb.get(0);

            //pre-calculate the width for the back layers
            float advance = 0;
            for (int i = 0; i < s.length(); ) {
                int c = s.codePointAt(i);
                i += Character.charCount(c);

                int renderC = (obf && !Character.isSpaceChar(c)) ? getRandomCodepoint(c) : c;
                advance += width(renderC) + (bold ? boldOffset : 0f);

                if (!obf && i < s.length())
                    advance += getKerning(c, s.codePointAt(i));
            }
            float finalX = initialX + advance;

            //track Z layer
            int zi = 0;

            //render background
            if (bg) {
                float x0 = initialX + x;
                float y0 = initialY + y - lineHeight - descent;
                float w  = finalX - initialX;
                float h  = lineHeight;

                if (italic)
                    w += italicOffset;

                if (outline) {
                    x0 -= shadowOffset; y0 -= shadowOffset;
                    w += shadowOffset * 2; h += shadowOffset * 2;
                } else if (shadow) {
                    w += shadowOffset; h += shadowOffset;
                }

                int bgc = style.getBackgroundColor();
                consumer.consume(rectangle(matrices, x0, y0, x0 + w, y0 + h, zOffset * zi++, bgc));
            }

            //render outline
            if (outline) {
                int oc = style.getOutlineColor();
                for (int i = -shadowOffset; i <= shadowOffset; i += shadowOffset) {
                    for (int j = -shadowOffset; j <= shadowOffset; j += shadowOffset) {
                        if (i == 0 && j == 0)
                            continue;

                        xb.put(0, initialX + i); yb.put(0, initialY + j);
                        bakeString(consumer, matrices, s, italic, bold, obf, under, strike, x, y, zOffset * zi, oc, italicOffset, boldOffset);
                    }
                }
                zi++;
            }

            //render shadow
            if (shadow) {
                xb.put(0, initialX + shadowOffset); yb.put(0, initialY + shadowOffset);
                int sc = style.getShadowColor();
                bakeString(consumer, matrices, s, italic, bold, obf, under, strike, x, y, zOffset * zi++, sc, italicOffset, boldOffset);
            }

            //main render
            xb.put(0, initialX); yb.put(0, initialY);
            int color = style.getColor();
            bakeString(consumer, matrices, s, italic, bold, obf, under, strike, x, y, zOffset * zi, color, italicOffset, boldOffset);

            //prepare buffer data for the next word
            xb.put(0, finalX); yb.put(0, initialY);
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
            int pageStart = owner.getCodepointPage(codepoint);
            GlyphPage page = owner.glyphPages.get(pageStart);
            if (page != null)
                return page.charData.get(codepoint - pageStart).xadvance();
        }

        //no owner, use missing char data
        return missingCharPage.charData.get(0).xadvance();
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

    private record GlyphPage(int textureID, int width, int height, STBTTPackedchar.Buffer charData) {
        void free() {
            glDeleteTextures(textureID);
            charData.free();
        }
    }
}
