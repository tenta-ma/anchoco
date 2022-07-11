package org.tantama.anchoco.springcrib.m3u8.ffmpeg;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;

import org.bytedeco.javacpp.Loader;

/**
 * 動画変換処理
 */
@Slf4j
public class ConvertFfmpeg {

    // @see https://github.com/bytedeco/javacpp-presets/tree/master/ffmpeg

    /** 動画フォルダroot(でも相対パスでできそう) */
    private static final String movieRootPath = "C:/user/movies";

    private static final String index_file_extention = ".m3u8";

    /**
     * 変換処理
     *
     * @param targetPath     変換ファイル格納先フォルダ
     * @param targetFileName 変換元ファイル
     */
    public void createTsSegment(String targetPath, String targetFileName) {
        log.info("start");
        // file
        File movieFile = Paths.get(movieRootPath, targetFileName).toFile();
        // ファイル名はtargetFileNameから拡張子抜いたやつにしたい
        File outputFile = Paths.get(movieRootPath, targetPath, targetPath + index_file_extention).toFile();

        File outputFolder = Paths.get(movieRootPath, targetPath).toFile();

        try {

            // libraryのnativeなffmpegからのコマンド実行
            String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
            ProcessBuilder pb = new ProcessBuilder(ffmpeg,
                    "-i", movieFile.getAbsolutePath(),
                    "-c:v", "libx264",
                    "-c:a", "copy",
                    "-hls_time", "10",
                    "-hls_playlist_type", "vod",
                    "-hls_segment_filename", outputFolder.getAbsolutePath() + File.separator + "hogefuga%3d.ts",
                    outputFile.getAbsolutePath());
            pb.inheritIO().start().waitFor();

        } catch (InterruptedException e) {
            log.error("InterruptedException error.", e);
        } catch (IOException e) {
            log.error("IOException error.", e);
        } finally {
            log.info("end");
        }

    }

    /**
     * サムネイル作成
     *
     * @param targetPath     変換ファイル格納先フォルダ
     * @param targetFileName 変換元ファイル
     */
    public void createThumbnail(String targetPath, String targetFileName) {
        log.info("start");
        // file
        File movieFile = Paths.get(movieRootPath, targetFileName).toFile();
        File outputFolder = Paths.get(movieRootPath, targetPath).toFile();

        try {
            String filePath = outputFolder.getAbsolutePath() + File.separator + "thumbnail.jpg";

            String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
            // option :
            // http://mobilehackerz.jp/archive/wiki/index.php?%BA%C7%BF%B7ffmpeg%A4%CE%A5%AA%A5%D7%A5%B7%A5%E7%A5%F3%A4%DE%A4%C8%A4%E1

            // 時間とか指定しないと、先頭から100フレーム内で、もっともサムネイルにふさわしい絵をthumbnail.jpgに保存のような感じっぽい
            ProcessBuilder pb = new ProcessBuilder(ffmpeg,
                    // 「loglevel」 : ログレベル、まあみてもよくわかんないし、quiet(ログださない)
                    // 作成できなくてもプログラム上ではエラーにならないのでerrorにした
                    // "-loglevel", "error",
                    // 「-y」 : overwriteでもnot asking。バックグランド処理にするので、この設定は必須
                    "-y",
                    // 「-ss」 : 秒数、この場合3秒～かららしい
                    "-ss", "5",
                    // 「-i」 : input file (resource)
                    "-i", movieFile.getAbsolutePath(),
                    // "scale", "640:480",
                    // 「-s」 : サイズ "NNNxNNN"で横x縦の指定。例：よくあるPCモニター"1920x1080"
                    // "-s", "375x667",
                    // 「-aspect」 : アスペクト比 "16:9"のような形で指定、つけてみたけどよくわからなかった
                    // "-aspect", "3:8",
                    // 「-vf」 : 「-filter：v」のalias、よくわからなかった
                    // vf指定「scale」 : サイズを指定できる 「-s」に似ている 「scale=375:667」
                    // 640:-1とやると、横幅を640にして、元のアスペクト比を保持してできる、だってさ
                    // "-vf", "thumbnail",
                    "-vf", "scale=3860:-1",
                    // 「-frames:v, -vframes」 : 取得するframe数、サムネならframeで十分なので1
                    "-frames:v", "1",
                    // 「-qscale」 : 「-b」だとビットレート指定。大雑把に言うと、画質のqu固定量子化係数
                    // "-qscale", "32",
                    // 「-q:v」 : jpegの圧縮率 1～32のようである
                    "-q:v", "32",
                    filePath);
            pb.inheritIO().start().waitFor();

            log.info("create thumbnail : {}", filePath);

        } catch (InterruptedException e) {
            log.error("InterruptedException error.", e);
        } catch (IOException e) {
            log.error("IOException error.", e);
        } finally {
            log.info("end");
        }

    }

}
