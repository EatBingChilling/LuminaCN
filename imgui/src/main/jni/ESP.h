#ifndef ESP_H
#define ESP_H

#include <jni.h>
#include <vector>
#include "ImGui/imgui.h"

// Structure for 3D vector
struct Vector3f {
    float x, y, z;
};

// Structure for entity data
struct EntityData {
    Vector3f position;
    int type; // 0: PLAYER, 1: ENEMY, 2: NEUTRAL
    float heightOffset;
};

// Global state declarations
extern bool g_enabled;
extern Vector3f g_playerPos;
extern float g_playerYaw;
extern float g_playerPitch;
extern std::vector<EntityData> g_entities;
extern float g_lineWidth;
extern pthread_mutex_t g_dataMutex;

// Settings namespace
namespace ESPSettings {
    extern float BoxLineWidth;
    extern float TracerLineWidth;
    extern float MaxDistance;
}

// Function declarations
void DrawESPOverlay();
extern "C" {
JNIEXPORT void JNICALL Java_com_mycompany_application_GLES3JNIView_setESPOverlayEnabled(JNIEnv* env, jclass cls, jboolean enabled);
JNIEXPORT void JNICALL Java_com_mycompany_application_GLES3JNIView_updateESPPlayerPosition(JNIEnv* env, jclass cls, jfloat x, jfloat y, jfloat z, jfloat yaw, jfloat pitch);
JNIEXPORT void JNICALL Java_com_mycompany_application_GLES3JNIView_updateESPEntities(JNIEnv* env, jclass cls, jobjectArray entities);
JNIEXPORT void JNICALL Java_com_mycompany_application_GLES3JNIView_setESPLineWidth(JNIEnv* env, jclass cls, jfloat width);
}

#endif // ESP_H