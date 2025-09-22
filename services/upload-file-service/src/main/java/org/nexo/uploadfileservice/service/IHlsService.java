package org.nexo.uploadfileservice.service;

import java.io.File;
import java.io.IOException;

public interface IHlsService {
    File convertToHls(File inputFile, String outputDir) throws IOException, InterruptedException;
}
