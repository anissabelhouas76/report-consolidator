package dz.ab.reportconsolidator.service;
import dz.ab.reportconsolidator.exception.InvalidWordDocumentException;
import dz.ab.reportconsolidator.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.util.*;
@Service
public class ReportProcessingService {
 private final WordExtractionService extraction;
 private final ConsolidationService consolidation;
 private final WordGenerationService generation;
 private final int maxFiles;
 private final long maxFileBytes, maxTotalBytes;
 public ReportProcessingService(WordExtractionService extraction, ConsolidationService consolidation,
   WordGenerationService generation, @Value("${report.max-files:100}") int maxFiles,
   @Value("${report.max-file-bytes:10485760}") long maxFileBytes,
   @Value("${report.max-total-bytes:104857600}") long maxTotalBytes) {
  this.extraction = extraction; this.consolidation = consolidation; this.generation = generation;
  this.maxFiles = maxFiles; this.maxFileBytes = maxFileBytes; this.maxTotalBytes = maxTotalBytes;
 }
 public byte[] process(MultipartFile[] files) {
  validate(files);
  List<ExtractedReport> reports = new ArrayList<>();
  for (MultipartFile file : files) {
   try (InputStream input = file.getInputStream()) { reports.add(extraction.extract(input, safeName(file))); }
   catch (IOException e) { throw new InvalidWordDocumentException("Impossible de lire le fichier " + safeName(file) + "."); }
  }
  return generation.generate(consolidation.consolidate(reports));
 }
 private void validate(MultipartFile[] files) {
  if (files == null || files.length == 0) throw new InvalidWordDocumentException("Sélectionnez au moins un fichier .docx.");
  if (files.length > maxFiles) throw new InvalidWordDocumentException("La limite est de " + maxFiles + " fichiers par consolidation.");
  long total = 0;
  for (MultipartFile file : files) {
   if (file == null) throw new InvalidWordDocumentException("Un fichier fourni est invalide.");
   String name = safeName(file);
   String lower = name.toLowerCase(Locale.ROOT);
   if (lower.endsWith(".doc")) throw new InvalidWordDocumentException("Le fichier " + name + " doit être converti de .doc en .docx.");
   if (!lower.endsWith(".docx")) throw new InvalidWordDocumentException("Le fichier " + name + " doit avoir le format .docx.");
   if (file.isEmpty()) throw new InvalidWordDocumentException("Le fichier " + name + " est vide.");
   if (file.getSize() > maxFileBytes) throw new InvalidWordDocumentException("Le fichier " + name + " dépasse la taille autorisée (10 Mo par défaut).");
   total += file.getSize();
   if (total > maxTotalBytes) throw new InvalidWordDocumentException("La taille totale des fichiers dépasse la limite autorisée (100 Mo par défaut).");
  }
 }
 private String safeName(MultipartFile file) {
  String name = Objects.toString(file.getOriginalFilename(), "fichier");
  name = name.replace('\\', '/');
  name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
  return name.length() > 180 ? name.substring(name.length() - 180) : name;
 }
}