package cinnamon.render.texture;

import cinnamon.Client;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Pair;
import cinnamon.utils.Resource;
import cinnamon.utils.TextureIO;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static cinnamon.events.Events.LOGGER;
import static cinnamon.render.texture.Texture.TextureParams.MIPMAP;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

public class AnimatedTexture extends Texture {

    private static final List<AnimatedTexture> ANIMATED_TEXTURES = new ArrayList<>();

    protected final AnimationData animationData;
    protected final ByteBuffer imgBuffer, frameBuffer;
    protected final int totalWidth;
    protected final boolean mipmap;

    protected boolean tick = false;
    private int currentFrame;

    protected AnimatedTexture(int id, int width, int height, int totalWidth, boolean mipmap, ByteBuffer imgBuffer, ByteBuffer frameBuffer, AnimationData animationData) {
        super(id, width, height);
        this.totalWidth = totalWidth;
        this.mipmap = mipmap;
        this.imgBuffer = imgBuffer;
        this.frameBuffer = frameBuffer;
        this.animationData = animationData;
        ANIMATED_TEXTURES.add(this);
        applyFrame(0, 0, 0f);
    }

    protected static Texture loadTexture(TextureIO.ImageData image, Resource animation, int params) {
        AnimationData anim = AnimationData.load(animation, image.width, image.height);
        if (anim == null)
            return null;

        ByteBuffer buffer = MemoryUtil.memAlloc(4 * anim.frameWidth * anim.frameHeight);
        ByteBuffer imgBuffer = MemoryUtil.memAlloc(image.buffer.capacity());
        imgBuffer.put(image.buffer);
        imgBuffer.rewind();
        image.buffer.rewind();

        int id = Texture.registerTexture(anim.frameWidth, anim.frameHeight, buffer, params);
        return new AnimatedTexture(id, anim.frameWidth, anim.frameHeight, image.width, MIPMAP.has(params), imgBuffer, buffer, anim);
    }

    public static void tickAll() {
        for (AnimatedTexture texture : ANIMATED_TEXTURES)
            texture.tick();
    }

    public static void freeAll() {
        ANIMATED_TEXTURES.clear();
    }

    @Override
    public int getID() {
        //texture has been accessed, so it will be used somewhere
        tick = true;
        return super.getID();
    }

    public void tick() {
        if (!tick) return;

        int currentFrame = 0, nextFrame = 0;
        float delta = 0f;
        int currentTime = (int) (Client.getInstance().ticks % animationData.totalTime);

        for (int i = 0, timeSum = 0; i < animationData.framesIDTime.size(); i++) {
            Pair<Integer, Integer> frame = animationData.framesIDTime.get(i);
            int time = frame.second();
            timeSum += time;

            if (timeSum > currentTime) {
                currentFrame = frame.first();
                Pair<Integer, Integer> next = animationData.framesIDTime.get((i + 1) % animationData.framesIDTime.size());
                nextFrame = next.first();
                delta = (float) (currentTime - (timeSum - time)) / time;
                break;
            }
        }

        if (this.currentFrame != currentFrame || animationData.interpolate)
            applyFrame(currentFrame, nextFrame, delta);

        this.currentFrame = currentFrame;
        this.tick = false;
    }

    @Override
    public void free() {
        MemoryUtil.memFree(imgBuffer);
        MemoryUtil.memFree(frameBuffer);
        super.free();
    }

    protected void applyFrame(int currentFrame, int nextFrame, float delta) {
        int width = getWidth() * 4;
        int totalWidth = this.totalWidth * 4;
        int frameSize = width * getHeight();
        boolean interpolate = animationData.interpolate && delta > 0f && nextFrame != currentFrame;

        int xFrames = totalWidth / width;
        int yOffset = totalWidth * getHeight();
        int currentBufferPos = currentFrame % xFrames * width + currentFrame / xFrames * yOffset;
        int nextBufferPos = nextFrame % xFrames * width + nextFrame / xFrames * yOffset;

        frameBuffer.clear();

        for (int i = 0; i < frameSize; i++) {
            //fix index to match matrix layout
            int j = i % width + i / width * totalWidth;

            if (!interpolate) {
                frameBuffer.put(imgBuffer.get(currentBufferPos + j));
                continue;
            }

            int currentColor = imgBuffer.get(currentBufferPos + j) & 0xFF;
            int nextColor = imgBuffer.get(nextBufferPos + j) & 0xFF;
            int interpolatedColor = (int) (currentColor + (nextColor - currentColor) * delta);
            frameBuffer.put((byte) interpolatedColor);
        }

        frameBuffer.rewind();
        updateBuffer();
    }

    protected void updateBuffer() {
        glBindTexture(GL_TEXTURE_2D, getID());
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, getWidth(), getHeight(), GL_RGBA, GL_UNSIGNED_BYTE, frameBuffer);
        if (mipmap) glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    protected record AnimationData(int frameWidth, int frameHeight, boolean interpolate, int totalTime, List<Pair<Integer, Integer>> framesIDTime) {
        public static AnimationData load(Resource res, int width, int height) {
            LOGGER.debug("Loading animation data \"%s\"", res);

            InputStream stream = IOUtils.getResource(res);
            if (stream == null) {
                LOGGER.error("Resource not found \"%s\"", res);
                return null;
            }

            try (stream; InputStreamReader reader = new InputStreamReader(stream)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                int frameWidth = json.has("width") ? json.get("width").getAsInt() : width;
                int frameHeight = json.has("height") ? json.get("height").getAsInt() : height;

                if (frameWidth <= 0 || width % frameWidth != 0 || frameHeight <= 0 || height % frameHeight != 0) {
                    LOGGER.error("Invalid animation frame size (%d x %d) for \"%s\"", frameWidth, frameHeight, res);
                    return null;
                }

                int frameCount = (width / frameWidth) * (height / frameHeight);
                if (frameCount <= 1) {
                    LOGGER.debug("Ignoring animation with 1 or less frames (%d)", frameCount);
                    return null;
                }

                boolean interpolate = json.has("interpolate") && json.get("interpolate").getAsBoolean();
                int frameTime = json.has("frametime") ? json.get("frametime").getAsInt() : 1;

                List<Pair<Integer, Integer>> framesIDTime = new ArrayList<>();
                int totalTime = 0;

                if (!json.has("frames")) {
                    for (int i = 0; i < frameCount; i++) {
                        framesIDTime.add(new Pair<>(i, frameTime));
                        totalTime += frameTime;
                    }
                } else {
                    for (JsonElement frame : json.getAsJsonArray("frames")) {
                        int frameID, time;

                        if (frame.isJsonPrimitive()) {
                            frameID = frame.getAsInt();
                            time = frameTime;
                        } else {
                            JsonObject frameObj = frame.getAsJsonObject();
                            frameID = frameObj.get("frame").getAsInt();
                            time = frameObj.get("time").getAsInt();

                            if (time <= 0) {
                                LOGGER.debug("Skipping invalid frame time (%d) for frame ID %d", time, frameID);
                                continue;
                            }
                        }

                        if (frameID < 0 || frameID >= frameCount) {
                            LOGGER.debug("Skipping invalid frame ID %d", frameID);
                            continue;
                        }

                        framesIDTime.add(new Pair<>(frameID, time));
                        totalTime += time;
                    }
                }

                return new AnimationData(frameWidth, frameHeight, interpolate, totalTime, framesIDTime);
            } catch (Exception e) {
                LOGGER.error("Failed to load animation data \"%s\"", res, e);
                return null;
            }
        }
    }
}
