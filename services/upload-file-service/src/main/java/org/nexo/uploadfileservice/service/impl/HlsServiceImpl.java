package org.nexo.uploadfileservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.nexo.uploadfileservice.service.IHlsService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
@Slf4j
public class HlsServiceImpl implements IHlsService {
    @Override
    public File convertToHls(File inputFile, String outputDir) throws IOException, InterruptedException {
        File outDir = new File(outputDir);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

//        String ffmpegPath = "D:\\App_1\\ffmage\\ffmpeg-8.0-full_build\\ffmpeg-8.0-full_build\\bin\\ffmpeg.exe";
//
//        // Tạo file index.m3u8 trong thư mục output
//        File outputM3u8 = new File(outDir, "index.m3u8");
//
//        String command = String.format(
//                "\"%s\" -i \"%s\" -c:v libx264 -c:a aac -profile:v baseline -level 3.0 -s 640x360 " +
//                        "-start_number 0 -hls_time 6 -hls_list_size 0 " +
//                        "-hls_segment_filename \"%s\\segment_%%03d.ts\" \"%s\"",
//                ffmpegPath,
//                inputFile.getAbsolutePath(),
//                outDir.getAbsolutePath(),
//                outputM3u8.getAbsolutePath()
//        );

//        String os = System.getProperty("os.name").toLowerCase();
//        String[] cmd;
//        if (os.contains("win")) {
//            cmd = new String[]{"cmd.exe", "/c", command};
//        } else {
//            cmd = new String[]{"bash", "-c", command};
//        }
        String ffmpegPath = "D:\\App_1\\ffmage\\ffmpeg-8.0-full_build\\ffmpeg-8.0-full_build\\bin\\ffmpeg.exe";
        String inputFilePath = inputFile.getAbsolutePath();
        File outputM3u8File = new File(outDir, "index.m3u8");
        String outputM3u8Path = outputM3u8File.getAbsolutePath();
        String segmentPattern = new File(outDir, "segment_%03d.ts").getAbsolutePath();

        String[] cmd = {
                "cmd.exe", "/c",
                ffmpegPath,
                "-i", inputFilePath,
                "-c:v", "libx264",
                "-c:a", "aac",
                "-profile:v", "baseline",
                "-level", "3.0",
                "-s", "640x360",
                "-start_number", "0",
                "-hls_time", "6",
                "-hls_list_size", "0",
                "-hls_segment_filename", segmentPattern,
                outputM3u8Path
        };
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed with exit code " + exitCode);
        }

        return outputM3u8File;
    }

}
