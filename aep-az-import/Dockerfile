FROM ubuntu:22.04

# Install dependencies
RUN apt-get update && apt-get install -y \
    curl \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Download and install AzCopy for Linux ARM64 (Apple Silicon)
RUN curl -fsSL "https://aka.ms/downloadazcopy-v10-linux-arm64" -o /tmp/azcopy.tar.gz \
    && tar -xzf /tmp/azcopy.tar.gz -C /tmp \
    && find /tmp -name "azcopy" -type f -exec mv {} /usr/local/bin/azcopy \; \
    && chmod +x /usr/local/bin/azcopy \
    && rm -rf /tmp/azcopy*

# Create working directories
RUN mkdir -p /data /logs

# Copy and configure upload script
COPY scripts/upload.sh /usr/local/bin/upload.sh
RUN chmod +x /usr/local/bin/upload.sh

ENTRYPOINT ["/usr/local/bin/upload.sh"]
