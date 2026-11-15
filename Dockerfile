FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && apt-get install -y --no-install-recommends \
    xvfb \
    x11vnc \
    fluxbox \
    novnc \
    websockify \
    && rm -rf /var/lib/apt/lists/* \
    && ln -s /usr/share/novnc/vnc_lite.html /usr/share/novnc/index.html

WORKDIR /app
COPY . .
RUN find . -name "*.java" > sources.txt && javac -d out @sources.txt

ENV DISPLAY=:0
ENV RESOLUTION=1150x750x16

RUN printf '#!/bin/bash\n\
Xvfb :0 -screen 0 $RESOLUTION &\n\
sleep 1\n\
fluxbox &\n\
x11vnc -display :0 -nopw -listen localhost -xkb -forever &\n\
websockify --web=/usr/share/novnc/ 8080 localhost:5900 &\n\
java -Xmx350m -cp /app/out PortfolioApp\n' > /start.sh && chmod +x /start.sh

EXPOSE 8080
CMD ["/start.sh"]
