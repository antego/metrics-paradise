FROM openjdk:8
ENTRYPOINT ["/usr/bin/java", "-jar", "/usr/share/metrics-paradise/metrics-paradise.jar"]

ADD target/lib /usr/share/metrics-paradise/lib
ADD target/metrics-paradise.jar /usr/share/metrics-paradise/metrics-paradise.jar