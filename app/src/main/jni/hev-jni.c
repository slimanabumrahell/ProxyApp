#include <jni.h>

extern int hev_socks5_tunnel_main_from_file(const char *config_path, int tun_fd);
extern void hev_socks5_tunnel_quit(void);

JNIEXPORT void JNICALL
Java_com_example_proxyapp_TunnelBridge_start(JNIEnv *env, jclass clazz,
                                             jstring path, jint fd) {
    const char *p = (*env)->GetStringUTFChars(env, path, 0);
    hev_socks5_tunnel_main_from_file(p, fd);
    (*env)->ReleaseStringUTFChars(env, path, p);
}

JNIEXPORT void JNICALL
Java_com_example_proxyapp_TunnelBridge_stop(JNIEnv *env, jclass clazz) {
    hev_socks5_tunnel_quit();
}
