package cf.wayzer.mindustry;

import cf.wayzer.libraryManager.LibraryManager;

import java.net.URLClassLoader;

public class Loader {
    static void load(LibraryManager man) throws Exception {
        man.loadToClassLoader((URLClassLoader) Loader.class.getClassLoader());
    }
}
