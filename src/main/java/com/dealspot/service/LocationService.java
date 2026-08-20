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

    /**
     * Returns all 31 Karnataka districts with both English and Kannada names.
     */
    public List<String[]> getDistricts() {
        return KARNATAKA_DISTRICTS;
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
