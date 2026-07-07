package cinnamon.text;

import cinnamon.utils.IOUtils;

import java.nio.file.Path;

public interface ClickEvent {
    void onClick();

    class OpenUrl implements ClickEvent {
        private final String url;

        public OpenUrl(String url) {
            this.url = url;
        }

        @Override
        public void onClick() {
            IOUtils.openURL(url);
        }
    }

    class OpenFile implements ClickEvent {
        private final Path filePath;

        public OpenFile(Path filePath) {
            this.filePath = filePath;
        }

        @Override
        public void onClick() {
            IOUtils.openFile(filePath);
        }
    }
}
