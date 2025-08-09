#include <jni.h>
#include "imgui.h"

extern "C" {

JNIEXPORT void JNICALL
Java_com_project_lumina_client_game_EspNative_drawLine(
    JNIEnv*, jclass,
    jfloat x1, jfloat y1, jfloat x2, jfloat y2,
    jint color, jfloat thick) {
    ImGui::GetBackgroundDrawList()->AddLine(
        ImVec2(x1, y1), ImVec2(x2, y2), color, thick);
}

JNIEXPORT void JNICALL
Java_com_project_lumina_client_game_EspNative_drawRect(
    JNIEnv*, jclass,
    jfloat x, jfloat y, jfloat w, jfloat h,
    jint color, jfloat thick) {
    ImGui::GetBackgroundDrawList()->AddRect(
        ImVec2(x, y), ImVec2(x + w, y + h), color, 0.0f, 0, thick);
}

}
