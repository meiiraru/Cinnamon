package cinnamon.text;

import cinnamon.render.MatrixStack;
import cinnamon.utils.UIHelper;

public interface HoverEvent {
    void onHover(MatrixStack matrices, int mouseX, int mouseY, float delta);

    class ShowText implements HoverEvent {
        private final Text text;

        public ShowText(Text text) {
            this.text = text;
        }

        @Override
        public void onHover(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            matrices.pushMatrix();
            matrices.translate(0, 0, 5f);
            UIHelper.renderTooltip(matrices, mouseX, mouseY, text);
            matrices.popMatrix();
        }
    }
}
