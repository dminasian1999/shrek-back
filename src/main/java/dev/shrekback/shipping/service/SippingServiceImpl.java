package dev.shrekback.shipping.service;

import dev.shrekback.post.dto.exceptions.PostNotFoundException;
import dev.shrekback.shipping.dao.ShippingCountriesRepository;
import dev.shrekback.shipping.dao.ShippingECORepository;
import dev.shrekback.shipping.dao.ShippingEMSRepository;
import dev.shrekback.shipping.model.Country;
import dev.shrekback.shipping.model.EcoPost;
import dev.shrekback.shipping.model.Ems;
import dev.shrekback.shipping.model.ShippingCountry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SippingServiceImpl implements SippingService {
    final ShippingCountriesRepository countriesRepository;
    final ShippingEMSRepository emsRepository;
    final ShippingECORepository ecoRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * First index that *looks* like a number (after cleaning currency).
     */
    private static int firstNumericIndex(String[] row) {
        for (int i = 0; i < row.length; i++) {
            String s = row[i];
            if (looksNumeric(s)) return i;
        }
        return -1;
    }

    private static boolean looksNumeric(String s) {
        if (s == null) return false;
        s = s.trim();
        s = s.replaceAll("[^0-9.\\-]", ""); // strip ₪, ?, spaces, etc.
        return !s.isEmpty() && s.matches("-?\\d+(\\.\\d+)?");
    }

    /**
     * Parse prices like "₪ 47.00", "?47.00", " 47 ", or empty -> 0.0 (adjust if you prefer null).
     */
    private static double parseMoney(String cell) {
        if (cell == null) return 0.0;
        String cleaned = cell.replaceAll("[^0-9.\\-]", "");
        if (cleaned.isEmpty()) return 0.0;
        return Double.parseDouble(cleaned);
    }

    /**
     * Reads next non-empty, non-whitespace line.
     */
    @Override
    public boolean addEco(String index, String csv) {
        try (BufferedReader br = new BufferedReader(new FileReader(csv))) {
            String headerLine = nextDataLine(br);
            String upTo100Line = nextDataLine(br);
            String upTo250Line = nextDataLine(br);
            String upTo500Line = nextDataLine(br);
            String upTo750Line = nextDataLine(br);
            String upTo1000Line = nextDataLine(br);
            String upTo1500Line = nextDataLine(br);
            String upTo2000Line = nextDataLine(br);

            String[] headers = splitCells(headerLine);
            String[] p100 = splitCells(upTo100Line);
            String[] p250 = splitCells(upTo250Line);
            String[] p500 = splitCells(upTo500Line);
            String[] p750 = splitCells(upTo750Line);
            String[] p1000 = splitCells(upTo1000Line);
            String[] p1500 = splitCells(upTo1500Line);
            String[] p2000 = splitCells(upTo2000Line);

            // first numeric column in the first price row; if none, start at 0
            int startCol = firstNumericIndex(p100);
            if (startCol < 0) startCol = 0; // <-- don't skip col 0

            // align columns by taking the minimum length across rows
            int maxCols = Math.min(headers.length,
                    Math.min(p100.length, Math.min(p250.length,
                            Math.min(p500.length, Math.min(p750.length,
                                    Math.min(p1000.length, Math.min(p1500.length, p2000.length)))))));
            int n = maxCols - startCol;

            List<EcoPost> batch = new ArrayList<>(n);
            for (int c = 0; c < n; c++) {
                int i = startCol + c;

                EcoPost eco = new EcoPost();

                // header in your sheet is just "1","2","3"... use it as both groupId and itemsFrom
                String hdr = headers[i] == null ? "" : headers[i].trim();
                if (hdr.isEmpty()) hdr = String.valueOf(c + 1);

                eco.setGroupId(hdr);
                eco.setPriceUpTo100g(parseMoney(p100[i]));
                eco.setPriceUpTo250g(parseMoney(p250[i]));
                eco.setPriceUpTo500g(parseMoney(p500[i]));
                eco.setPriceUpTo750g(parseMoney(p750[i]));
                eco.setPriceUpTo1000g(parseMoney(p1000[i]));
                eco.setPriceUpTo1500g(parseMoney(p1500[i]));
                eco.setPriceUpTo2000g(parseMoney(p2000[i]));

                batch.add(eco);
            }

            ecoRepository.saveAll(batch);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String extractLeadingInt(String text) {
        if (text == null) return "";
        Matcher m = Pattern.compile("(\\d+)").matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private static String[] splitCells(String line, String sep) {
        String[] raw = line.split(java.util.regex.Pattern.quote(sep), -1);
        for (int i = 0; i < raw.length; i++) raw[i] = raw[i].trim();
        return raw;
    }

    private static int parseIntSafe(String s) {
        if (s == null) return 0;
        String cleaned = s.replaceAll("[^0-9-]", ""); // מוריד ₪, רווחים, פסיקים
        if (cleaned.isEmpty() || cleaned.equals("-")) return 0;
        return Integer.parseInt(cleaned);
    }

    private static double parseDoubleSafe(String s) {
        if (s == null) return 0.0;
        String cleaned = s.replace("₪", "").replace(",", "").trim();
        if (cleaned.isEmpty()) return 0.0;
        return Double.parseDouble(cleaned);
    }

    @Override
    public boolean addEms(String index, String csv) {
        try (BufferedReader br = new BufferedReader(new FileReader(csv))) {
            String headerLine = nextDataLine(br);
            String halfKgLine = nextDataLine(br);
            String oneKgLine = nextDataLine(br);
            String addHalfLine = nextDataLine(br);

            if (headerLine == null || halfKgLine == null || oneKgLine == null || addHalfLine == null) {
                System.err.println("EMS CSV must contain 4 lines (header + 3 price rows).");
                return false;
            }

            String[] groups = splitCells(headerLine);
            String[] price05 = splitCells(halfKgLine);
            String[] price10 = splitCells(oneKgLine);
            String[] eachAdd05 = splitCells(addHalfLine);

            int n = groups.length;
            if (price05.length != n || price10.length != n || eachAdd05.length != n) {
                System.err.printf("Column mismatch: header=%d, 0.5kg=%d, 1kg=%d, +0.5kg=%d%n",
                        n, price05.length, price10.length, eachAdd05.length);
                return false;
            }

            List<Ems> toSave = new ArrayList<>(n);
            for (int col = 0; col < n; col++) {
                Ems ems = new Ems();
                ems.setGroupId(groups[col].isEmpty() ? String.valueOf(col + 1) : groups[col]);

                ems.setPriceUpTo500g(parseDouble(price05[col]));
                ems.setPriceUpTo1000g(parseDouble(price10[col]));
                ems.setEachAdditional500g(parseDouble(eachAdd05[col]));

                toSave.add(ems);
            }

            emsRepository.saveAll(toSave);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- helpers ---
    private static String nextDataLine(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.trim().isEmpty()) return line;
        }
        return null;
    }

    private static String[] splitCells(String line) {
        String[] raw = line.split(",", -1);
        for (int i = 0; i < raw.length; i++) raw[i] = raw[i].trim();
        return raw;
    }

    private static double parseDouble(String cell) {
        if (cell == null) return 0.0;
        String cleaned = cell.replace("₪", "").trim();
        if (cleaned.isEmpty()) return 0.0;
        return Double.parseDouble(cleaned);
    }

    /**
     * Parses "0-500" → [0,500]; returns null for single numbers like "500".
     */
    private WeightBand parseWeightBand(String cell) {
        if (cell == null) return null;
        cell = cell.trim();
        int dash = cell.indexOf('-');
        if (dash <= 0) return null; // single value (e.g., "500") — surcharge row
        try {
            int from = Integer.parseInt(cell.substring(0, dash).trim());
            int to = Integer.parseInt(cell.substring(dash + 1).trim());
            return new WeightBand(from, to);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses "170", "170.00", "₪170" etc.; returns null if blank or invalid.
     */
    private Double parsePrice(String cell) {
        if (cell == null) return null;
        cell = cell.replace("₪", "").trim();
        if (cell.isEmpty()) return null;
        try {
            return Double.parseDouble(cell);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class WeightBand {
        final int fromGrams;
        final int toGrams;

        WeightBand(int fromGrams, int toGrams) {
            this.fromGrams = fromGrams;
            this.toGrams = toGrams;
        }
    }


    private List<Country> findByWeightRangeEms(int from, int to) {
        Query query = new Query();
        query.addCriteria(Criteria.where("emsPackages")
                .elemMatch(Criteria.where("weightFrom").lte(from)
                        .and("weightTo").gte(to)));
        return mongoTemplate.find(query, Country.class);
    }

    @Override
    public boolean addCountries(String index, String csv) {
        try (BufferedReader br = new BufferedReader(new FileReader(csv))) {
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",");
                ShippingCountry country = new ShippingCountry();
                country.setCountryName(fields[2]);
                country.setGroupIdEcoPost(safeParseInt(fields[1]));
                country.setGroupIdESM(safeParseInt(fields[0]));
                countriesRepository.save(country);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private int safeParseInt(String value) {
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("x")) {
            return 0;
        }
        return (int) Double.parseDouble(value);
    }


    @Override
    public Double getShippingCost(String countryName, double weight) {
        ShippingCountry country = countriesRepository.findById(countryName).orElseThrow(PostNotFoundException::new);
    System.out.println(country);
        double ress=0.0;
        if (country.getGroupIdEcoPost() > 0 && weight <= 2000) {
         EcoPost eco =  ecoRepository.findById(String.valueOf(country.getGroupIdEcoPost())).orElseThrow(PostNotFoundException::new);
            if (weight <= 100) {
                ress =  eco.getPriceUpTo100g();
            } else if (weight <= 250) {
                ress = eco.getPriceUpTo250g();
            } else if (weight <= 500) {
                ress = eco.getPriceUpTo500g();
            } else if (weight <= 750) {
                ress = eco.getPriceUpTo750g();
            } else if (weight <= 1000) {
                ress = eco.getPriceUpTo1000g();
            } else if (weight <= 1500) {
                ress = eco.getPriceUpTo1500g();
            } else if (weight <= 2000) {
                ress = eco.getPriceUpTo2000g();
            } else {
                throw new IllegalArgumentException("Weight not supported: " + weight);
            }

        }

        if ((country.getGroupIdESM() > 0)) {
            Ems ems =  emsRepository.findById(String.valueOf(country.getGroupIdESM())).orElseThrow(PostNotFoundException::new);
            if (weight <= 500) {
                ress = ems.getPriceUpTo500g();
            } else if (weight <= 1000) {
                ress = ems.getPriceUpTo1000g();
            } else if (weight > 1000) {
                double res = weight-1000;
                double res1 = res%500;
                System.out.println(res1);
                double res2 = res1 * ems.getEachAdditional500g();
                ress = ems.getPriceUpTo1000g()+res2;
            }

        }

        return ress;

    }
}
