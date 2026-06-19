# Android APK 多阶段构建 Dockerfile
# 阶段1: 构建环境
FROM ubuntu:22.04 AS builder

RUN apt-get update && apt-get install -y \
    openjdk-17-jdk wget unzip git curl && rm -rf /var/lib/apt/lists/*

ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools

RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools && \
    cd /tmp && curl -sL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o cmdline-tools.zip && \
    unzip -q cmdline-tools.zip && mv cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest && rm cmdline-tools.zip

RUN yes | sdkmanager --licenses > /dev/null 2>&1 || true && \
    sdkmanager --install "platform-tools" "platforms;android-35" "build-tools;35.0.0" > /dev/null 2>&1 || true

COPY . /project
WORKDIR /project
RUN chmod +x gradlew && ./gradlew assembleDebug --no-daemon

# 阶段2: 输出镜像
FROM ubuntu:22.04 AS output
RUN apt-get update && apt-get install -y openjdk-17-jre && rm -rf /var/lib/apt/lists/*
COPY --from=builder /project/app/build/outputs/apk/debug/app-debug.apk /apk/
RUN chown -R appuser:appgroup /apk && ls -lh /apk/

RUN addgroup -g 1001 -S appgroup && adduser -u 1001 -S appuser -G appgroup
USER appuser
CMD ["ls", "-lh", "/apk/"]