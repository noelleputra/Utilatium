package dev.noellx.inventarium.managers;



public class PluginManager {
    private static PluginManager instance;
    
    private PluginManager() {}
    
    public static PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    public void initialize() {
        // ButtonLockManager & ConfigManager pakai lazy singleton sendiri (getInstance()),
        // jadi tidak perlu di-init manual di sini. Tambahkan manager lain di sini
        // kalau nanti perlu ada state yang di-load saat startup.
    }
}
