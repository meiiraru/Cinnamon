package cinnamon.utils;

import cinnamon.Cinnamon;
import cinnamon.Client;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.nfd.NFDFilterItem;
import org.lwjgl.util.nfd.NFDOpenDialogArgs;
import org.lwjgl.util.nfd.NFDPathSetEnum;
import org.lwjgl.util.nfd.NFDPickFolderArgs;
import org.lwjgl.util.nfd.NFDSaveDialogArgs;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFWNativeCocoa.glfwGetCocoaWindow;
import static org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window;
import static org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Window;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.nfd.NativeFileDialog.*;

public class FileDialog {

    private static Pair<Integer, Long> getWindowHandle() {
        Client c = Client.getInstance();
        if (c.window == null)
            throw new RuntimeException("Client is not initialized");

        long window = c.window.getHandle();

        return switch (Cinnamon.PLATFORM) {
            case FREEBSD, LINUX -> new Pair<>(NFD_WINDOW_HANDLE_TYPE_X11, glfwGetX11Window(window));
            case MACOSX -> new Pair<>(NFD_WINDOW_HANDLE_TYPE_COCOA, glfwGetCocoaWindow(window));
            case WINDOWS -> new Pair<>(NFD_WINDOW_HANDLE_TYPE_WINDOWS, glfwGetWin32Window(window));
        };
    }

    private static String getHomeDir() {
        return System.getProperty("user.home");
    }

    public static String openFile(Filter... filters) {
        return openFile(getHomeDir(), filters);
    }

    public static String openFile(String defaultPath, Filter... filters) {
        try (MemoryStack stack = stackPush(); NFDFilterItem.Buffer filterBuffer = NFDFilterItem.malloc(filters.length)) {
            //filters
            for (int i = 0; i < filters.length; i++) {
                filterBuffer.get(i)
                        .name(stack.UTF8(filters[i].description))
                        .spec(stack.UTF8(filters[i].fileTypes));
            }

            //open dialog
            PointerBuffer outPath = stack.mallocPointer(1);
            Pair<Integer, Long> window = getWindowHandle();
            int result = NFD_OpenDialog_With(outPath, NFDOpenDialogArgs
                    .calloc(stack)
                    .filterList(filterBuffer)
                    .defaultPath(stack.UTF8(defaultPath))
                    .parentWindow(it -> it.type(window.first()).handle(window.second()))
            );

            //return path
            return parseSingleResult(result, outPath);
        }
    }

    public static List<String> openFiles(Filter... filters) {
        return openFiles(getHomeDir(), filters);
    }

    public static List<String> openFiles(String defaultPath, Filter... filters) {
        try (MemoryStack stack = stackPush(); NFDFilterItem.Buffer filterBuffer = NFDFilterItem.malloc(filters.length)) {
            //filters
            for (int i = 0; i < filters.length; i++) {
                filterBuffer.get(i)
                        .name(stack.UTF8(filters[i].description))
                        .spec(stack.UTF8(filters[i].fileTypes));
            }

            //open dialog
            PointerBuffer outPath = stack.mallocPointer(1);
            Pair<Integer, Long> window = getWindowHandle();
            int result = NFD_OpenDialogMultiple_With(outPath, NFDOpenDialogArgs
                    .calloc(stack)
                    .filterList(filterBuffer)
                    .defaultPath(stack.UTF8(defaultPath))
                    .parentWindow(it -> it.type(window.first()).handle(window.second()))
            );

            //return paths
            return parseMultipleResult(result, outPath, stack);
        }
    }

    public static String openFolder() {
        return openFolder(getHomeDir());
    }

    public static String openFolder(String defaultPath) {
        try (MemoryStack stack = stackPush()) {
            //open dialog
            PointerBuffer outPath = stack.mallocPointer(1);
            Pair<Integer, Long> window = getWindowHandle();
            int result = NFD_PickFolder_With(outPath, NFDPickFolderArgs
                    .calloc(stack)
                    .defaultPath(stack.UTF8(defaultPath))
                    .parentWindow(it -> it.type(window.first()).handle(window.second()))
            );

            //return path
            return parseSingleResult(result, outPath);
        }
    }

    public static List<String> openFolders() {
        return openFolders(getHomeDir());
    }

    public static List<String> openFolders(String defaultPath) {
        try (MemoryStack stack = stackPush()) {
            //open dialog
            PointerBuffer outPath = stack.mallocPointer(1);
            Pair<Integer, Long> window = getWindowHandle();
            int result = NFD_PickFolderMultiple_With(outPath, NFDPickFolderArgs
                    .calloc(stack)
                    .defaultPath(stack.UTF8(defaultPath))
                    .parentWindow(it -> it.type(window.first()).handle(window.second()))
            );

            //return path
            return parseMultipleResult(result, outPath, stack);
        }
    }

    public static String saveFile(Filter... filters) {
        return saveFile("", getHomeDir(), filters);
    }

    public static String saveFile(String defaultName, String defaultPath, Filter... filters) {
        try (MemoryStack stack = stackPush(); NFDFilterItem.Buffer filterBuffer = NFDFilterItem.malloc(filters.length)) {
            //filters
            for (int i = 0; i < filters.length; i++) {
                filterBuffer.get(i)
                        .name(stack.UTF8(filters[i].description))
                        .spec(stack.UTF8(filters[i].fileTypes));
            }

            //open dialog
            PointerBuffer outPath = stack.mallocPointer(1);
            Pair<Integer, Long> window = getWindowHandle();
            int result = NFD_SaveDialog_With(outPath, NFDSaveDialogArgs
                    .calloc(stack)
                    .filterList(filterBuffer)
                    .defaultPath(stack.UTF8(defaultPath))
                    .defaultName(stack.UTF8(defaultName))
                    .parentWindow(it -> it.type(window.first()).handle(window.second()))
            );
            //return path
            return parseSingleResult(result, outPath);
        }
    }

    private static String parseSingleResult(int result, PointerBuffer outPath) {
        return switch (result) {
            case NFD_OKAY -> {
                String path = outPath.getStringUTF8(0);
                NFD_FreePath(outPath.get(0));
                yield path;
            }
            case NFD_CANCEL -> null;
            default -> throw new RuntimeException(NFD_GetError());
        };
    }

    private static List<String> parseMultipleResult(int result, PointerBuffer outPath, MemoryStack stack) {
        return switch (result) {
            case NFD_OKAY -> {
                List<String> paths = new ArrayList<>();

                long pathSet = outPath.get(0);
                NFDPathSetEnum psEnum = NFDPathSetEnum.calloc(stack);
                NFD_PathSet_GetEnum(pathSet, psEnum);

                while (NFD_PathSet_EnumNext(psEnum, outPath) == NFD_OKAY && outPath.get(0) != 0) {
                    paths.add(outPath.getStringUTF8(0));
                    NFD_PathSet_FreePath(outPath.get(0));
                }

                NFD_PathSet_FreeEnum(psEnum);
                NFD_PathSet_Free(pathSet);

                yield paths;
            }
            case NFD_CANCEL -> List.of();
            default -> throw new RuntimeException(NFD_GetError());
        };
    }

    public static class Filter {
        public static final Filter
                TEXT_FILES = new Filter("Text files", "txt"),
                IMAGE_FILES = new Filter("Image files", "png,jpg,jpeg,bmp,gif,hdr"),
                AUDIO_FILES = new Filter("Audio files", "ogg");

        private final String description, fileTypes;

        public Filter(String description, String fileTypes) {
            this.description = description;
            this.fileTypes = fileTypes;
        }
    }
}
