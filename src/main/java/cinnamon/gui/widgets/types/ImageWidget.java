package cinnamon.gui.widgets.types;

import cinnamon.gui.widgets.HoverWidget;
import cinnamon.math.MatrixStack;
import cinnamon.model.GeometryHelper;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.texture.Texture;
import cinnamon.utils.Resource;

public class ImageWidget extends HoverWidget {

    protected Resource image;
    protected boolean stretch;
    protected float u = 0f, v = 0f, regionWidth = 1f, regionHeight = 1f;

    public ImageWidget(int x, int y, int width, int height, Resource image) {
        this(x, y, width, height, image, false);
    }

    public ImageWidget(int x, int y, int width, int height, Resource image, boolean stretch) {
        super(x, y, width, height);
        this.image = image;
        this.stretch = stretch;
        setSelectable(false);
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderWidget(matrices, mouseX, mouseY, delta);

        if (image != null)
            renderImage(matrices, mouseX, mouseY, delta);
    }

    protected void renderImage(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (isStretch()) {
            VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, getX(), getY(), getWidth(), getHeight(), getU(), getV(), getRegionWidth(), getRegionHeight(), 1, 1), image);
        } else {
            Texture texture = Texture.of(image);
            float width = texture.getWidth() * getRegionWidth();
            float height = texture.getHeight() * getRegionHeight();

            //render texture centered in the widget keeping the aspect ratio
            float aspectRatio = width / height;
            float widgetAspectRatio = (float) getWidth() / getHeight();
            if (aspectRatio > widgetAspectRatio) {
                float newHeight = getWidth() / aspectRatio;
                float y = getY() + (getHeight() - newHeight) / 2;
                VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, getX(), y, getWidth(), newHeight, getU(), getV(), getRegionWidth(), getRegionHeight(), 1, 1), image);
            } else {
                float newWidth = getHeight() * aspectRatio;
                float x = getX() + (getWidth() - newWidth) / 2;
                VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, x, getY(), newWidth, getHeight(), getU(), getV(), getRegionWidth(), getRegionHeight(), 1, 1), image);
            }
        }
    }

    public void setStretch(boolean stretch) {
        this.stretch = stretch;
    }

    public boolean isStretch() {
        return stretch;
    }

    public void setImage(Resource image) {
        this.image = image;
    }

    public Resource getImage() {
        return image;
    }

    public void setUV(float u, float v) {
        setU(u);
        setV(v);
    }

    public void setU(float u) {
        this.u = u;
    }

    public void setV(float v) {
        this.v = v;
    }

    public float getU() {
        return u;
    }

    public float getV() {
        return v;
    }

    public void setRegion(float regionWidth, float regionHeight) {
        setRegionWidth(regionWidth);
        setRegionHeight(regionHeight);
    }

    public void setRegionWidth(float regionWidth) {
        this.regionWidth = regionWidth;
    }

    public void setRegionHeight(float regionHeight) {
        this.regionHeight = regionHeight;
    }

    public float getRegionWidth() {
        return regionWidth;
    }

    public float getRegionHeight() {
        return regionHeight;
    }
}
