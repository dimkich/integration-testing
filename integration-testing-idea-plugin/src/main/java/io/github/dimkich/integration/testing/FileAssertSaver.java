package io.github.dimkich.integration.testing;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class FileAssertSaver {
    private static final String SETTINGS_FILE = "settings.txt";
    private static final String TEMPLATE_FILE = "template.xml";
    private static final String EXPECTED_FILE_POSTFIX = "_expected.xml";
    private final String resultDir = System.getProperty("java.io.tmpdir") + "java_tests" + File.separator;

    public void save(Project project) throws IOException {
        File baseDir = new File(resultDir);
        if (!baseDir.exists()) {
            Messages.showErrorDialog(project, "Directory " + resultDir + " is not exists", "Error Saving Assertion File");
        }
        for (File dir : Objects.requireNonNull(baseDir.listFiles(File::isDirectory))) {
            List<String> list;
            try (Stream<String> stream = Files.lines(Paths.get(dir.getPath() + File.separator + SETTINGS_FILE))) {
                list = stream.toList();
            }
            String originalFilePath = list.get(0);
            String itemId = list.get(1).replace("<testCase", "");

            String line;
            StringWriter stringWriter = new StringWriter();
            BufferedWriter writer = new BufferedWriter(stringWriter);
            try (BufferedReader template = new BufferedReader(new FileReader(
                    dir.getPath() + File.separator + TEMPLATE_FILE, StandardCharsets.UTF_8))) {
                while ((line = template.readLine()) != null) {
                    int i = line.indexOf(itemId);
                    if (i < 0) {
                        writer.write(line);
                        writer.newLine();
                    } else {
                        String temp = line.substring(i + itemId.length());
                        int fileNum = Integer.parseInt(temp.substring(0, temp.indexOf("\"")));
                        String spaces = " ".repeat(countBeginningSpaces(line));

                        VirtualFile expected = LocalFileSystem.getInstance().findFileByNioFile(
                                Path.of(dir.getPath() + File.separator + fileNum + EXPECTED_FILE_POSTFIX));
                        expected.setCharset(StandardCharsets.UTF_8);
                        VfsUtil.markDirtyAndRefresh(false, false, false, expected);
                        Document document = FileDocumentManager.getInstance().getDocument(expected);
                        BufferedReader testCase = new BufferedReader(new StringReader(document.getText()));
                        while ((line = testCase.readLine()) != null) {
                            writer.write(spaces + line);
                            writer.newLine();
                        }
                    }
                }
                writer.flush();
            }
            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByNioFile(Path.of(originalFilePath));
            WriteCommandAction.runWriteCommandAction(project, "Integration Tests " + new Date(), null, () -> {
                try {
                    virtualFile.setWritable(true);
                    virtualFile.setCharset(StandardCharsets.UTF_8);
                    virtualFile.setBinaryContent(stringWriter.toString().getBytes(StandardCharsets.UTF_8));
                    VfsUtil.markDirtyAndRefresh(false, false, false, virtualFile);
                } catch (IOException e) {
                    Messages.showErrorDialog(project, e.getClass().getName() + " " + e.getMessage(), "Error Saving Assertion File");
                }
            });
        }
    }

    private int countBeginningSpaces(String s) {
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) != ' ') {
                break;
            }
            i++;
        }
        return i;
    }
}
