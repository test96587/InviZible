/*
    This file is part of InviZible Pro.

    InviZible Pro is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    InviZible Pro is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with InviZible Pro.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2019-2025 by Garmatin Oleksandr invizible.soft@gmail.com
 */

package pan.alexander.tordnscrypt.data.log_reader;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import pan.alexander.tordnscrypt.utils.filemanager.FileShortener;
import pan.alexander.tordnscrypt.utils.filemanager.FileManager;

import static pan.alexander.tordnscrypt.utils.logger.Logger.loge;
import static pan.alexander.tordnscrypt.utils.logger.Logger.logi;
import static pan.alexander.tordnscrypt.utils.logger.Logger.logw;

public class OwnFileReader {
    //private final static long TOO_LONG_FILE_LENGTH = 1024 * 100;
    private final static int MAX_LINES_QUANTITY = 80;
    private final static int MAX_LINES_HYSTERESIS = 50;


    private static final ReentrantLock reentrantLock = new ReentrantLock();

    private final Context context;
    private final String filePath;

    public OwnFileReader(Context context, String filePath) {
        this.context = context;
        this.filePath = filePath;
    }

    public List<String> readLastLines() {

        List<String> lines = new LinkedList<>();

        boolean fileIsTooLong = false;

        try {
            reentrantLock.lockInterruptibly();

            File file = new File(filePath);

            if (!file.exists()) {
                return Collections.emptyList();
            }

            if (context != null && !file.canRead()) {
                if (!file.setReadable(true)) {
                    logw("Impossible to read file " + filePath + " Try restore access");

                    FileManager fileManager = new FileManager();
                    fileManager.restoreAccess(context, filePath);
                }

                if (file.canRead()) {
                    logi("Access to " + filePath + " restored");
                } else {
                    loge("Impossible to read file " + filePath);
                }
            }

            FileShortener.shortenTooTooLongFile(filePath);

            try (FileInputStream fstream = new FileInputStream(filePath);
                 InputStreamReader reader = new InputStreamReader(fstream);
                 BufferedReader br = new BufferedReader(reader)) {

                for (String line; (line = br.readLine()) != null; ) {
                    lines.add(line);
                    if (lines.size() > MAX_LINES_QUANTITY + MAX_LINES_HYSTERESIS) {
                        lines.remove(0);
                        fileIsTooLong = true;
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        return lines;
                    }
                }
            }

            if (fileIsTooLong) {
                lines = lines.subList(lines.size() - MAX_LINES_QUANTITY, lines.size());
                shortenTooLongFile(lines);
            }

        }  catch (Exception e) {
            loge("Impossible to read file " + filePath, e);
        } finally {
            reentrantLock.unlock();
        }

        return lines;
    }

    private void shortenTooLongFile(List<String> lines) {
        File file = new File(filePath);
        if (!file.isFile()) {
            return;
        }

        try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
            if (lines != null && lines.size() != 0) {
                StringBuilder buffer = new StringBuilder();
                for (String line : lines) {
                    buffer.append(line).append("\n");
                }
                writer.println(buffer);
            }
        } catch (IOException e) {
            loge("Unable to rewrite too long file" + filePath, e);
        }
    }

    void updateLines(List<String> lines) {
        File file = new File(filePath);
        if (!file.isFile()) {
            return;
        }

        try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
            if (lines != null && !lines.isEmpty()) {
                StringBuilder buffer = new StringBuilder();
                for (String line : lines) {
                    buffer.append(line).append("\n");
                }
                writer.println(buffer);
            }
        } catch (IOException e) {
            loge("Unable to update lines" + filePath, e);
        }
    }

    long getFileLength() {
        try {
            reentrantLock.lockInterruptibly();

            File file = new File(filePath);
            if (!file.isFile() || !file.canRead()) {
                return -1;
            }

            return file.length();
        } catch (Exception e) {
            loge("OwnFileReader getFileSize", e);
        } finally {
            if (reentrantLock.isLocked() && reentrantLock.isHeldByCurrentThread()) {
                reentrantLock.unlock();
            }
        }
        return -1;
    }

}
