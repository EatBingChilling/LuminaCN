/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * This is open source — not open credit.
 *
 * If you're here to build, welcome. If you're here to repaint and reupload
 * with your tag slapped on it… you're not fooling anyone.
 *
 * Changing colors and class names doesn't make you a developer.
 * Copy-pasting isn't contribution.
 *
 * You have legal permission to fork. But ask yourself — are you improving,
 * or are you just recycling someone else's work to feed your ego?
 *
 * Open source isn't about low-effort clones or chasing clout.
 * It's about making things better. Sharper. Cleaner. Smarter.
 *
 * So go ahead, fork it — but bring something new to the table,
 * or don’t bother pretending.
 *
 * This message is philosophical. It does not override your legal rights under GPLv3.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * GPLv3 Summary:
 * - You have the freedom to run, study, share, and modify this software.
 * - If you distribute modified versions, you must also share the source code.
 * - You must keep this license and copyright intact.
 * - You cannot apply further restrictions — the freedom stays with everyone.
 * - This license is irrevocable, and applies to all future redistributions.
 *
 * Full text: https://www.gnu.org/licenses/gpl-3.0.html
 */

#include <jni.h>
#include <cmath>
#include <vector>
#include <algorithm>
#include "ImGui/imgui.h"
#include <android/log.h>
#include <GLES3/gl3.h>
#include "ESP.h"

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "ESP", __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "ESP", __VA_ARGS__))

// External screen dimensions from ArrayList.cpp
extern int screenWidth;
extern int screenHeight;

// Global state definitions
bool g_enabled = false;
Vector3f g_playerPos = {0.0f, 0.0f, 0.0f};
float g_playerYaw = 0.0f;
float g_playerPitch = 0.0f;
std::vector<EntityData> g_entities;
float g_lineWidth = 1.5f;
pthread_mutex_t g_dataMutex = PTHREAD_MUTEX_INITIALIZER;

// Settings namespace definitions
namespace ESPSettings {
    float BoxLineWidth = 2.0f;
    float TracerLineWidth = 1.5f;
    float MaxDistance = 100.0f; // For tracer opacity
    float MinScale = 0.2f;
    float MaxScale = 1.0f;
}

// Utility functions
static float CalculateDistance(const Vector3f& pos1, const Vector3f& pos2) {
    return std::sqrt(
            std::pow(pos1.x - pos2.x, 2) +
            std::pow(pos1.y - pos2.y, 2) +
            std::pow(pos1.z - pos2.z, 2)
    );
}

static ImU32 GetPriorityColor(int entityType, float alpha) {
    switch (entityType) {
        case 1: // ENEMY
            return ImGui::GetColorU32(ImVec4(1.0f, 0.0f, 0.0f, alpha)); // Red
        case 0: // PLAYER
            return ImGui::GetColorU32(ImVec4(0.0f, 1.0f, 0.0f, alpha)); // Green
        case 2: // NEUTRAL
            return ImGui::GetColorU32(ImVec4(1.0f, 1.0f, 0.0f, alpha)); // Yellow
        default:
            return ImGui::GetColorU32(ImVec4(0.0f, 1.0f, 1.0f, alpha)); // Cyan
    }
}

static float CalculateTracerOpacity(float distance) {
    return std::max(0.0f, 1.0f - (distance / ESPSettings::MaxDistance));
}

static float CalculateScaleFactor(float distance) {
    return std::max(ESPSettings::MinScale, std::min(ESPSettings::MaxScale, 1.0f / (distance * 0.1f + 1.0f)));
}

static bool ProjectToScreen(const Vector3f& position, const Vector3f& playerPos, float yaw, float pitch,
                            float screenWidth, float screenHeight, ImVec2& outScreenPos) {
    Vector3f relativePos = {
            position.x - playerPos.x,
            position.y - playerPos.y,
            position.z - playerPos.z
    };

    // Rotate based on player's yaw (convert to radians)
    float yawRad = yaw * (M_PI / 180.0f);
    float rotatedX = relativePos.x * cos(yawRad) + relativePos.z * sin(yawRad);
    float rotatedZ = -relativePos.x * sin(yawRad) + relativePos.z * cos(yawRad);

    // Rotate based on player's pitch
    float pitchRad = pitch * (M_PI / 180.0f);
    float rotatedY = relativePos.y * cos(pitchRad) - rotatedZ * sin(pitchRad);
    float projectedZ = relativePos.y * sin(pitchRad) + rotatedZ * cos(pitchRad);

    // If behind player, don't draw
    if (projectedZ <= 0.1f) return false;

    // Project to 2D screen coordinates with distance-based scaling
    float distance = CalculateDistance(position, playerPos);
    float scale = CalculateScaleFactor(distance);
    float screenX = screenWidth / 2.0f + (rotatedX / projectedZ) * (screenWidth * 0.5f) * scale;
    float screenY = screenHeight / 2.0f - (rotatedY / projectedZ) * (screenHeight * 0.5f) * scale;

    // Check screen bounds with a buffer
    if (screenX < -50 || screenX > screenWidth + 50 || screenY < -50 || screenY > screenHeight + 50) {
        return false;
    }

    outScreenPos = ImVec2(screenX, screenY);
    return true;
}

// Draw ESP elements
void DrawESPOverlay() {
    if (!g_enabled) return;

    ImDrawList* drawList = ImGui::GetBackgroundDrawList();
    ImVec2 screenCenter(screenWidth / 2.0f, screenHeight / 2.0f);
    float halfWidth = 0.3f * ESPSettings::MaxScale;
    float boxHeight = 1.8f * ESPSettings::MaxScale;

    pthread_mutex_lock(&g_dataMutex);
    for (const auto& entity : g_entities) {
        Vector3f entityPos = entity.position;

        // Tracer rendering (aligned with player view)
        ImVec2 screenPos;
        if (ProjectToScreen(entityPos, g_playerPos, g_playerYaw, g_playerPitch, screenWidth, screenHeight, screenPos)) {
            float distance = CalculateDistance(entityPos, g_playerPos);
            float alpha = CalculateTracerOpacity(distance);
            ImU32 color = GetPriorityColor(entity.type, alpha);

            // Calculate view direction vector
            float yawRad = g_playerYaw * (M_PI / 180.0f);
            float pitchRad = g_playerPitch * (M_PI / 180.0f);
            Vector3f viewDir = {
                    cos(yawRad) * cos(pitchRad),
                    sin(pitchRad),
                    sin(yawRad) * cos(pitchRad)
            };
            Vector3f tracerStart = g_playerPos;
            tracerStart.y += 1.6f; // Eye level adjustment
            ImVec2 tracerEnd = screenPos;

            drawList->AddLine(screenCenter, tracerEnd, color, ESPSettings::TracerLineWidth * alpha);
        }

        // Box rendering with scaled size
        Vector3f corners[2] = {
                {entityPos.x - halfWidth, entityPos.y, entityPos.z - halfWidth},
                {entityPos.x + halfWidth, entityPos.y + boxHeight, entityPos.z + halfWidth}
        };

        ImVec2 screenCorners[2];
        int validCorners = 0;
        for (int i = 0; i < 2; ++i) {
            if (ProjectToScreen(corners[i], g_playerPos, g_playerYaw, g_playerPitch, screenWidth, screenHeight, screenCorners[validCorners])) {
                validCorners++;
            }
        }

        if (validCorners == 2) {
            if (screenCorners[0].x + screenCorners[0].y > screenCorners[1].x + screenCorners[1].y) {
                std::swap(screenCorners[0], screenCorners[1]);
            }

            ImVec2 min = screenCorners[0];
            ImVec2 max = screenCorners[1];
            ImU32 color = GetPriorityColor(entity.type, 1.0f);
            drawList->AddRect(min, max, color, 0.0f, 0, ESPSettings::BoxLineWidth);
        }
    }
    pthread_mutex_unlock(&g_dataMutex);
}

// JNI functions
extern "C" {
JNIEXPORT void JNICALL
Java_com_mycompany_application_GLES3JNIView_setESPOverlayEnabled(JNIEnv* env, jclass cls, jboolean enabled) {
    pthread_mutex_lock(&g_dataMutex);
    g_enabled = enabled;
    pthread_mutex_unlock(&g_dataMutex);
    LOGI("ESP Overlay %s", enabled ? "enabled" : "disabled");
}

JNIEXPORT void JNICALL
Java_com_mycompany_application_GLES3JNIView_updateESPPlayerPosition(JNIEnv* env, jclass cls,
                                                                    jfloat x, jfloat y, jfloat z, jfloat yaw, jfloat pitch) {
    pthread_mutex_lock(&g_dataMutex);
    g_playerPos = {x, y, z};
    g_playerYaw = yaw;
    g_playerPitch = pitch;
    pthread_mutex_unlock(&g_dataMutex);
}

JNIEXPORT void JNICALL
Java_com_mycompany_application_GLES3JNIView_updateESPEntities(JNIEnv* env, jclass cls, jobjectArray entities) {
    pthread_mutex_lock(&g_dataMutex);
    g_entities.clear();

    jsize len = env->GetArrayLength(entities);
    for (jsize i = 0; i < len; ++i) {
        jobject entityObj = env->GetObjectArrayElement(entities, i);
        if (entityObj == nullptr) {
            LOGE("Null entity object at index %d", i);
            continue;
        }

        jclass entityClass = env->GetObjectClass(entityObj);
        if (entityClass == nullptr) {
            LOGE("Failed to get class for entity object at index %d", i);
            env->DeleteLocalRef(entityObj);
            continue;
        }

        jfieldID posXField = env->GetFieldID(entityClass, "x", "F");
        jfieldID posYField = env->GetFieldID(entityClass, "y", "F");
        jfieldID posZField = env->GetFieldID(entityClass, "z", "F");
        jfieldID typeField = env->GetFieldID(entityClass, "type", "I");
        jfieldID heightOffsetField = env->GetFieldID(entityClass, "heightOffset", "F");

        if (posXField == nullptr || posYField == nullptr || posZField == nullptr ||
            typeField == nullptr || heightOffsetField == nullptr) {
            LOGE("Failed to get field IDs for entity object at index %d", i);
            env->DeleteLocalRef(entityClass);
            env->DeleteLocalRef(entityObj);
            continue;
        }

        EntityData entity;
        entity.position.x = env->GetFloatField(entityObj, posXField);
        entity.position.y = env->GetFloatField(entityObj, posYField);
        entity.position.z = env->GetFloatField(entityObj, posZField);
        entity.type = env->GetIntField(entityObj, typeField);
        entity.heightOffset = env->GetFloatField(entityObj, heightOffsetField);

        g_entities.push_back(entity);
        env->DeleteLocalRef(entityObj);
        env->DeleteLocalRef(entityClass);
    }
    pthread_mutex_unlock(&g_dataMutex);
}

JNIEXPORT void JNICALL
Java_com_mycompany_application_GLES3JNIView_setESPLineWidth(JNIEnv* env, jclass cls, jfloat width) {
    pthread_mutex_lock(&g_dataMutex);
    ESPSettings::TracerLineWidth = width;
    pthread_mutex_unlock(&g_dataMutex);
}


} // extern "C"