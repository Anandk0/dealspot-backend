package com.dealspot.service;

import com.dealspot.entity.User;
import com.dealspot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final UserRepository userRepository;

    private static final List<String[]> KARNATAKA_DISTRICTS = Arrays.asList(
            new String[]{"Bagalkot", "ಬಾಗಲಕೋಟೆ"},
            new String[]{"Ballari", "ಬಳ್ಳಾರಿ"},
            new String[]{"Belagavi", "ಬೆಳಗಾವಿ"},
            new String[]{"Bengaluru Rural", "ಬೆಂಗಳೂರು ಗ್ರಾಮಾಂತರ"},
            new String[]{"Bengaluru Urban", "ಬೆಂಗಳೂರು ನಗರ"},
            new String[]{"Bidar", "ಬೀದರ್"},
            new String[]{"Chamarajanagar", "ಚಾಮರಾಜನಗರ"},
            new String[]{"Chikkaballapura", "ಚಿಕ್ಕಬಳ್ಳಾಪುರ"},
            new String[]{"Chikkamagaluru", "ಚಿಕ್ಕಮಗಳೂರು"},
            new String[]{"Chitradurga", "ಚಿತ್ರದುರ್ಗ"},
            new String[]{"Dakshina Kannada", "ದಕ್ಷಿಣ ಕನ್ನಡ"},
            new String[]{"Davanagere", "ದಾವಣಗೆರೆ"},
            new String[]{"Dharwad", "ಧಾರವಾಡ"},
            new String[]{"Gadag", "ಗದಗ"},
            new String[]{"Hassan", "ಹಾಸನ"},
            new String[]{"Haveri", "ಹಾವೇರಿ"},
            new String[]{"Kalaburagi", "ಕಲಬುರಗಿ"},
            new String[]{"Kodagu", "ಕೊಡಗು"},
            new String[]{"Kolar", "ಕೋಲಾರ"},
            new String[]{"Koppal", "ಕೊಪ್ಪಳ"},
            new String[]{"Mandya", "ಮಂಡ್ಯ"},
            new String[]{"Mysuru", "ಮೈಸೂರು"},
            new String[]{"Raichur", "ರಾಯಚೂರು"},
            new String[]{"Ramanagara", "ರಾಮನಗರ"},
            new String[]{"Shivamogga", "ಶಿವಮೊಗ್ಗ"},
            new String[]{"Tumakuru", "ತುಮಕೂರು"},
            new String[]{"Udupi", "ಉಡುಪಿ"},
            new String[]{"Uttara Kannada", "ಉತ್ತರ ಕನ್ನಡ"},
            new String[]{"Vijayapura", "ವಿಜಯಪುರ"},
            new String[]{"Yadgir", "ಯಾದಗಿರಿ"},
            new String[]{"Vijayanagara", "ವಿಜಯನಗರ"}
    );

    // Taluks per district, keyed by the district's English name (matching the
    // district id slug used on the frontend). Used for the property form's
    // district -> taluk dependent dropdown.
    private static final java.util.Map<String, List<String>> KARNATAKA_TALUKS = buildTaluks();

    private static java.util.Map<String, List<String>> buildTaluks() {
        java.util.Map<String, List<String>> m = new java.util.LinkedHashMap<>();
        m.put("Bagalkot", Arrays.asList("Bagalkot", "Badami", "Bilgi", "Hungund", "Jamkhandi", "Mudhol", "Rabkavi Banhatti", "Guledgudda", "Ilkal"));
        m.put("Ballari", Arrays.asList("Ballari", "Hospet (Hosapete)", "Sandur", "Siraguppa", "Kurugodu", "Kampli"));
        m.put("Belagavi", Arrays.asList("Belagavi", "Athani", "Bailhongal", "Chikodi", "Gokak", "Hukkeri", "Khanapur", "Raibag", "Ramdurg", "Saundatti", "Kittur", "Mudalgi", "Nippani", "Kagwad"));
        m.put("Bengaluru Rural", Arrays.asList("Devanahalli", "Doddaballapura", "Hoskote", "Nelamangala"));
        m.put("Bengaluru Urban", Arrays.asList("Bengaluru North", "Bengaluru South", "Bengaluru East", "Anekal", "Yelahanka", "Krishnarajapura"));
        m.put("Bidar", Arrays.asList("Bidar", "Aurad", "Basavakalyan", "Bhalki", "Humnabad", "Chitguppa", "Hulsoor", "Kamalnagar"));
        m.put("Chamarajanagar", Arrays.asList("Chamarajanagar", "Gundlupet", "Kollegal", "Yelandur", "Hanur"));
        m.put("Chikkaballapura", Arrays.asList("Chikkaballapura", "Bagepalli", "Chintamani", "Gauribidanur", "Gudibanda", "Sidlaghatta", "Cheluru"));
        m.put("Chikkamagaluru", Arrays.asList("Chikkamagaluru", "Kadur", "Koppa", "Mudigere", "Narasimharajapura", "Sringeri", "Tarikere", "Ajjampura", "Kalasa"));
        m.put("Chitradurga", Arrays.asList("Chitradurga", "Challakere", "Hiriyur", "Holalkere", "Hosadurga", "Molakalmuru"));
        m.put("Dakshina Kannada", Arrays.asList("Mangaluru", "Bantwal", "Belthangady", "Puttur", "Sullia", "Moodabidri", "Kadaba", "Mulki", "Ullal"));
        m.put("Davanagere", Arrays.asList("Davanagere", "Channagiri", "Harihar", "Honnali", "Jagalur", "Nyamati"));
        m.put("Dharwad", Arrays.asList("Dharwad", "Hubballi", "Kalghatgi", "Kundgol", "Navalgund", "Annigeri", "Alnavar"));
        m.put("Gadag", Arrays.asList("Gadag-Betageri", "Mundargi", "Nargund", "Ron", "Shirahatti", "Lakshmeshwar", "Gajendragad"));
        m.put("Hassan", Arrays.asList("Hassan", "Alur", "Arkalgud", "Arsikere", "Belur", "Channarayapatna", "Hole Narasipura", "Sakleshpur"));
        m.put("Haveri", Arrays.asList("Haveri", "Byadgi", "Hangal", "Hirekerur", "Ranebennur", "Savanur", "Shiggaon", "Rattihalli"));
        m.put("Kalaburagi", Arrays.asList("Kalaburagi", "Afzalpur", "Aland", "Chincholi", "Chittapur", "Jevargi", "Sedam", "Kamalapur", "Shahabad", "Yadrami"));
        m.put("Kodagu", Arrays.asList("Madikeri", "Somwarpet", "Virajpet", "Kushalnagar", "Ponnampet"));
        m.put("Kolar", Arrays.asList("Kolar", "Bangarapet", "Malur", "Mulbagal", "Srinivaspur", "KGF (Robertsonpet)"));
        m.put("Koppal", Arrays.asList("Koppal", "Gangavathi", "Kushtagi", "Yelburga", "Kanakagiri", "Karatagi"));
        m.put("Mandya", Arrays.asList("Mandya", "Krishnarajpet", "Maddur", "Malavalli", "Nagamangala", "Pandavapura", "Srirangapatna"));
        m.put("Mysuru", Arrays.asList("Mysuru", "Hunsur", "Krishnarajanagara", "Nanjangud", "Periyapatna", "Tirumakudalu Narasipura", "Heggadadevanakote", "Saligrama", "Saragur"));
        m.put("Raichur", Arrays.asList("Raichur", "Deodurga", "Lingsugur", "Manvi", "Sindhanur", "Maski", "Sirwar"));
        m.put("Ramanagara", Arrays.asList("Ramanagara", "Channapatna", "Kanakapura", "Magadi", "Harohalli"));
        m.put("Shivamogga", Arrays.asList("Shivamogga", "Bhadravathi", "Hosanagara", "Sagar", "Shikaripura", "Sorab", "Thirthahalli"));
        m.put("Tumakuru", Arrays.asList("Tumakuru", "Chiknayakanhalli", "Gubbi", "Koratagere", "Kunigal", "Madhugiri", "Pavagada", "Sira", "Tiptur", "Turuvekere"));
        m.put("Udupi", Arrays.asList("Udupi", "Karkala", "Kundapura", "Byndoor", "Hebri", "Kaup", "Brahmavar"));
        m.put("Uttara Kannada", Arrays.asList("Karwar", "Ankola", "Bhatkal", "Haliyal", "Honnavar", "Kumta", "Mundgod", "Siddapur", "Sirsi", "Supa (Joida)", "Yellapur", "Dandeli"));
        m.put("Vijayapura", Arrays.asList("Vijayapura", "Basavana Bagewadi", "Indi", "Muddebihal", "Sindagi", "Devara Hippargi", "Chadachan", "Talikota", "Nidagundi", "Kolhar", "Tikota", "Babaleshwar"));
        m.put("Yadgir", Arrays.asList("Yadgir", "Shahapur", "Shorapur (Surpur)", "Gurmitkal", "Hunsagi", "Wadgera"));
        m.put("Vijayanagara", Arrays.asList("Hospet (Hosapete)", "Hagaribommanahalli", "Hoovina Hadagali", "Kottur", "Kudligi", "Harapanahalli"));
        return m;
    }

    /**
     * Returns all 31 Karnataka districts with both English and Kannada names.
     */
    public List<String[]> getDistricts() {
        return KARNATAKA_DISTRICTS;
    }

    /**
     * Returns the taluks for a given district (by English name). Empty if unknown.
     */
    public List<String> getTaluks(String districtEnglishName) {
        if (districtEnglishName == null) return java.util.Collections.emptyList();
        return KARNATAKA_TALUKS.getOrDefault(districtEnglishName, java.util.Collections.emptyList());
    }

    /**
     * Sets the preferred district for a user.
     */
    @Transactional
    public void setUserDistrict(Long userId, String district) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPreferredDistrict(district);
        userRepository.save(user);
    }

    /**
     * Returns the preferred district for a user, or null if not set.
     */
    public String getUserDistrict(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getPreferredDistrict();
    }
}
