package jp.goodenough;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    @TempDir
    Path tmp;

    @Test
    void run_returnsZeroAndCreatesOutput_when_specifyingOutputFileAndMultipleInputs() throws Exception {
        // Arrange
        Path dataDir = Paths.get("data", "Domain-Driven_Refactoring");
        Path out = tmp.resolve("merged.pdf");
        String[] args = new String[] {
                "-o", out.toString(),
                dataDir.resolve("0_0_Domain-Driven Refactoring.pdf").toString(),
                dataDir.resolve("0_1_Domain-Driven Refactoring.pdf").toString(),
                dataDir.resolve("0_2_Domain-Driven Refactoring.pdf").toString()
        };

        // Act
        int exit = new Main().run(args);

        // Assert
        assertEquals(0, exit, "正常終了コードを返すこと");
        assertTrue(Files.exists(out), "出力PDFが作成されていること");
        assertTrue(Files.size(out) > 0, "出力PDFが空でないこと");
    }

    @Test
    void run_usesDirectoryNameAsOutput_when_singleDirectoryWithoutOutputOption() throws Exception {
        // Arrange
        Path dataDir = Paths.get("data", "Domain-Driven_Refactoring");
        // 実行ディレクトリに出力される仕様のため、一時ディレクトリでカレントを一時的に切り替える
        Path originalCwd = Paths.get("").toAbsolutePath();
        try {
            System.setProperty("user.dir", tmp.toAbsolutePath().toString());
            String[] args = new String[] { originalCwd.resolve(dataDir).toString() };
            Path expectedOut = tmp.resolve("Domain-Driven_Refactoring.pdf");

            // Act
            int exit = new Main().run(args);

            // Assert
            assertEquals(0, exit, "正常終了コードを返すこと");
            assertTrue(Files.exists(expectedOut), "ディレクトリ名.pdf で出力されること");
            assertTrue(Files.size(expectedOut) > 0, "出力PDFが空でないこと");
        } finally {
            System.setProperty("user.dir", originalCwd.toString());
        }
    }

    @Test
    void run_returnsError_when_noInputs() {
        // Arrange
        String[] args = new String[] { };

        // Act
        int exit = new Main().run(args);

        // Assert
        assertEquals(1, exit, "引数なしは使用方法表示で終了コード1であること");
    }
}