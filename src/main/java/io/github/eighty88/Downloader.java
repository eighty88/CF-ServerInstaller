package io.github.eighty88;

import lombok.RequiredArgsConstructor;
import me.tongfei.progressbar.DelegatingProgressBarConsumer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@RequiredArgsConstructor
public class Downloader {
    private final String url;

    public boolean download(File file) {
        final Logger logger = Main.getLogger();
        final URL from;
        final long length;

        try {
            from = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) from.openConnection();
            length = connection.getContentLength();

            if (file.isDirectory()) {
                file = new File(file, FilenameUtils.getName(from.getPath()));
            }
        } catch (IOException e) {
            logger.error("Download failed. (" + e + ")");
            return false;
        }


        try (CountingInputStream is = new CountingInputStream(from.openStream());
            FileOutputStream os = new FileOutputStream(file);
            ProgressBar bar = new ProgressBarBuilder()
                    .setInitialMax(Math.floorDiv(length, 1000))
                    .setUpdateIntervalMillis(1000)
                    .setConsumer(new DelegatingProgressBarConsumer(logger::info))
                    .setStyle(ProgressBarStyle.ASCII)
                    .hideEta()
                    .setMaxRenderedLength(20)
                    .setUnit("KiB", 1)
                    .build()) {

            bar.setExtraMessage(Utils.alignNumber(file.getName(), 24, true));

            new Thread(() -> {
                try {
                    IOUtils.copy(is, os);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            while (is.getByteCount() < length) {
                bar.stepTo(Math.floorDiv(is.getByteCount(), 1000));
            }

            bar.stepTo(Math.floorDiv(is.getByteCount(), 1000));

            return true;
        } catch (IOException e) {
            logger.error("Download failed. (" + e + ")");
            return false;
        }
    }
}
