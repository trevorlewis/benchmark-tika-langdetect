package com.trevor;

import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.langdetect.TextLangDetector;
import org.apache.tika.language.LanguageIdentifier;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageWriter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class Benchmark {

    public static void main(String[] args) {
        String inputFile = "data/test-100.tsv";
        String outputFile = "data/output.tsv";
        if (args.length > 0) {
            inputFile = args[0];
            if (args.length > 1) {
                outputFile = args[1];
            }
        }

        String[] languages = {"da", "de", "el", "en", "es", "et", "fi", "fr",
                "hu", "is", "it", "nl", "no", "pl", "pt", "ru", "sv", "th"};
        int N = languages.length;
        HashMap<String, Integer> langMap = new HashMap<>();
        for (int i = 0; i < N; i++) {
            langMap.put(languages[i], i);
        }

        int[] count = new int[N];
        BigInteger[] totalWords = new BigInteger[N];
        BigInteger[] totalChars = new BigInteger[N];
        BigInteger[] totalTimeTika = new BigInteger[N];
        BigInteger[] totalTimeOptimaize = new BigInteger[N];
        BigInteger[] totalTimeText = new BigInteger[N];
        for (int i = 0; i < N; i++) {
            totalWords[i] = new BigInteger("0");
            totalChars[i] = new BigInteger("0");
            totalTimeTika[i] = new BigInteger("0");
            totalTimeOptimaize[i] = new BigInteger("0");
            totalTimeText[i] = new BigInteger("0");
        }

        int[][] confusionMatrixTika = new int[N][N];
        int[][] confusionMatrixOptimaize = new int[N][N];
        int[][] confusionMatrixText = new int[N][N];

        BufferedReader br = null;
        PrintWriter writer = null;

        try {
            LanguageDetector detectorText = new TextLangDetector();
            LanguageDetector detectorOptimaize = new OptimaizeLangDetector();
            detectorOptimaize.loadModels(new HashSet(Arrays.asList(languages)));

            LanguageWriter writerText = new LanguageWriter(detectorText);
            LanguageWriter writerOptimaize = new LanguageWriter(detectorOptimaize);

            writer = new PrintWriter(outputFile);
            br = new BufferedReader(new FileReader(inputFile));

            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split("\\t");
                if (data.length != 2) continue;
                String truth = data[0];
                String text = data[1];

                String resultTika, resultOptimaize, resultText;
                long startTime, endTime, durationTika, durationOptimaize, durationText;

                count[langMap.get(truth)] += 1;
                totalWords[langMap.get(truth)] = totalWords[langMap.get(truth)].add(BigInteger.valueOf(text.split(" ").length));
                totalChars[langMap.get(truth)] = totalChars[langMap.get(truth)].add(BigInteger.valueOf(text.length()));

                startTime = System.nanoTime();
                LanguageIdentifier identifier = new LanguageIdentifier(text);
                resultTika = identifier.getLanguage();
                endTime = System.nanoTime();
                durationTika = (endTime - startTime);
                totalTimeTika[langMap.get(truth)] = totalTimeTika[langMap.get(truth)].add(BigInteger.valueOf(durationTika));
                confusionMatrixTika[langMap.get(truth)][langMap.get(resultTika)] += 1;

                writerOptimaize.reset();
                writerOptimaize.append(text);
                startTime = System.nanoTime();
                resultOptimaize = detectorOptimaize.detect().getLanguage();
                endTime = System.nanoTime();
                durationOptimaize = (endTime - startTime);
                totalTimeOptimaize[langMap.get(truth)] = totalTimeOptimaize[langMap.get(truth)].add(BigInteger.valueOf(durationOptimaize));
                confusionMatrixOptimaize[langMap.get(truth)][langMap.get(resultOptimaize)] += 1;

                writerText.reset();
                writerText.append(text);
                startTime = System.nanoTime();
                resultText = detectorText.detect().getLanguage();
                endTime = System.nanoTime();
                durationText = (endTime - startTime);
                totalTimeText[langMap.get(truth)] = totalTimeText[langMap.get(truth)].add(BigInteger.valueOf(durationText));
                confusionMatrixText[langMap.get(truth)][langMap.get(resultText)] += 1;

                writer.println(truth + "\t"
                        + resultTika + "\t" + durationTika + "\t"
                        + resultOptimaize + "\t" + durationOptimaize + "\t"
                        + resultText + "\t" + durationText + "\t"
                        + text.split(" ").length + "\t" + text.length()
                );
            }

            printStats("Tika", languages, confusionMatrixTika);
            printStats("Optimaize", languages, confusionMatrixOptimaize);
            printStats("Text", languages, confusionMatrixText);

            System.out.println("Lang\tAvg_Words_Per_Article\tAvg_Chars_Per_Article\tAvg_Time_Optimaize\tAvg_Time_Tika\tAvg_Time_Text");
            for (int i = 0; i < N; i++) {
                System.out.println(languages[i] + "\t"
                        + totalWords[i].divide(BigInteger.valueOf(count[i])) + "\t"
                        + totalChars[i].divide(BigInteger.valueOf(count[i])) + "\t"
                        + totalTimeTika[i].divide(BigInteger.valueOf(count[i])) + "\t"
                        + totalTimeOptimaize[i].divide(BigInteger.valueOf(count[i])) + "\t"
                        + totalTimeText[i].divide(BigInteger.valueOf(count[i]))
                );
            }
            System.out.println("*time in nano seconds");

        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    private static void printStats(String title, String[] languages, int[][] confusionMatrix) {
        System.out.println(title);

        System.out.println("Confusion Matrix:");
        int N = languages.length;
        int total = 0;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                System.out.print(confusionMatrix[i][j] + " ");
                total += confusionMatrix[i][j];
            }
            System.out.println();
        }

        double accuracy = 0;
        double[] precision = new double[N];
        double[] recall = new double[N];
        double[] fscore = new double[N];

        for (int i = 0; i < N; i++) {
            accuracy += confusionMatrix[i][i];
        }
        accuracy /= total;
        System.out.println("Accuracy : " + accuracy);

        System.out.println("Lang\tPrecision\tRecall\tF-Score");
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                precision[i] += confusionMatrix[i][j];
                recall[i] += confusionMatrix[j][i];
            }
            precision[i] = confusionMatrix[i][i] / precision[i];
            recall[i] = confusionMatrix[i][i] / recall[i];
            fscore[i] = 2 * precision[i] * recall[i] / (precision[i] + recall[i]);
            System.out.println(languages[i] + "\t" + precision[i] + "\t" + recall[i] + "\t" + fscore[i]);
        }
        System.out.println();
    }
}
