FROM maven:3.6.3-openjdk-8

#maven:3.6.3-openjdk-8中更换源debian:10
RUN echo "deb http://security.debian.org/debian-security bullseye-security main contrib non-free" > /etc/apt/sources.list

RUN apt update -y && apt install -y git

#切换目录，指定源码克隆后保存位置
WORKDIR /

#拉取源代码
RUN git clone https://gitee.com/atuptown/datalinkx.git /datalinkx \
    && cd /datalinkx \
    && cp docker/settings.xml /usr/share/maven/conf \
    && mvn clean package -X -U -Dmaven.test.skip=true \
    && cd /datalinkx/flinkx \
    && mvn clean package -U -Dmaven.test.skip=true

#切换jar目录
WORKDIR /datalinkx

#挂载配置映射路径
VOLUME /datalinkx/datalinkx-server/config
VOLUME /datalinkx/datalinkx-job/config

#挂载flinkx配置地址
VOLUME /datalinkx/flinkx/flinkconf

#挂载flinkx插件地址
VOLUME /datalinkx/flinkx/syncplugins

#声明项目所用端口
EXPOSE 12345 23456 9999

#启动java项目程序
CMD sh /datalinkx/bin/start.sh
