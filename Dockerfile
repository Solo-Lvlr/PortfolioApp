FROM eclipse-temurin:17-jdk-jammy

# Install virtual display (Xvfb), window manager (fluxbox), VNC server, and noVNC
RUN apt-get update && apt-get install -y --no-install-recommends \
    xvfb \
    x11vnc \
    fluxbox \
    novnc \
    websockify \
    && rm -rf /var/lib/apt/lists/*

# Set up app directory and compile all Java files
WORKDIR /app
COPY . .
RUN find . -name "*.java" > sources.txt && javac -d out @sources.txt

# Set virtual screen resolution (slightly larger than your 1100x700 JFrame)
ENV DISPLAY=:0
ENV RESOLUTION=1280x800x24

# Create startup script to run virtual display, window manager, noVNC, and your Java app
RUN printf '#!/bin/bash\n\
Xvfb :0 -screen 0 $RESOLUTION &\n\
sleep 1\n\
fluxbox &\n\
x11vnc -display :0 -nopw -listen localhost -xkb -forever &\n\
websockify --web=/usr/share/novnc/ 8080 localhost:5900 &\n\
java -cp /app/out PortfolioApp\n' > /start.sh && chmod +x /start.sh

EXPOSE 8080
CMD ["/start.sh"]
