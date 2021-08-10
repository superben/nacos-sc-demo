FROM openjdk:11-jre-slim

LABEL description="your description"
ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    APP_SLEEP=0 \
    JAVA_OPTS="" \
    RUN_ARGS="" \
    APP_NAME_ID=""

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
ENV TZ "Asia/Shanghai"

RUN mkdir -p /root/app/

WORKDIR /root/app/
COPY entrypoint.sh \
     target/app.jar \
     /root/app/

RUN chmod 755 entrypoint.sh 

ENTRYPOINT ["./entrypoint.sh"]
EXPOSE 8080
