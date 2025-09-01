package dev.shrekback.shipping.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "shrek-bulk_mail_rates")
@Getter
@Setter
@NoArgsConstructor
public class BulkMailRate {
    @Id
    private String weightBand; // e.g. "0-100", "101-250" (מפתח)

    private int weightFromGr;
    private int weightToGr;

    private double over5000WithFile;
    private double from1001to5000WithFile;
    private double from501to1000WithFile;
    private double from101to500WithFile;
    private double from50to100WithFile;
    private double over100NoFile;
    private double from50to100NoFile;
    private double from26to49NoFile;
    private double from5to25NoFile;
    private double from1to4NoFile;
}
