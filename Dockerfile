FROM --platform=linux/amd64 eclipse-temurin:17-jdk

ARG GRADLE_VERSION=8.10.2
ARG CMDLINE_TOOLS=11076708

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH="${PATH}:/opt/gradle/bin:${ANDROID_HOME}/cmdline-tools/latest/bin"

RUN apt-get update && apt-get install -y --no-install-recommends unzip curl \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt && mv /opt/gradle-${GRADLE_VERSION} /opt/gradle && rm /tmp/gradle.zip

RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS}_latest.zip" -o /tmp/tools.zip \
    && unzip -q /tmp/tools.zip -d ${ANDROID_HOME}/cmdline-tools && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm /tmp/tools.zip \
    && yes | sdkmanager --licenses >/dev/null \
    && sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"

WORKDIR /project
CMD ["gradle", "assembleDebug", "--no-daemon"]
