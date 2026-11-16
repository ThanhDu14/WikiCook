package com.wikicook.auth.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.auth.dto.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * FR-004, research R3: 8-128 code points after NFC, a letter, a digit, and not the email. Every violation has its own
 * code, which becomes {@code errors[].code} (NFR-007).
 */
class ValidPasswordValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    record Form(@ValidPassword String password) {
    }

    private List<String> codes(String password) {
        return validator.validate(new Form(password)).stream().map(ConstraintViolation::getMessage).sorted().toList();
    }

    /** The class-level rules, through a registration form. */
    private Set<String> registrationErrors(String email, String password, String confirm) {
        return validator.validate(new RegisterRequest("An", email, password, confirm)).stream()
                .map(v -> v.getPropertyPath() + ":" + v.getMessage()).collect(Collectors.toSet());
    }

    @Test
    void aValidPasswordHasNoViolation() {
        assertThat(codes("abc12345")).isEmpty();
        assertThat(codes("Mật khẩu số 1")).isEmpty();
    }

    @Test
    void sevenCodePointsIsTooShortAndEightPasses() {
        assertThat(codes("abcde12")).containsExactly("PASSWORD_TOO_SHORT");
        assertThat(codes("abcdef12")).isEmpty();
    }

    @Test
    void oneHundredTwentyEightCodePointsPassAndOneHundredTwentyNineIsTooLong() {
        assertThat(codes("a1" + "b".repeat(126))).isEmpty();
        assertThat(codes("a1" + "b".repeat(127))).containsExactly("PASSWORD_TOO_LONG");
    }

    @Test
    void emojiAreCountedAsCodePointsNotUtf16Units() {
        // 126 emoji + a letter + a digit = 128 code points, 254 UTF-16 units: must pass
        assertThat(codes("😀".repeat(126) + "a1")).isEmpty();
        assertThat(codes("😀".repeat(127) + "a1")).containsExactly("PASSWORD_TOO_LONG");
    }

    @Test
    void lengthIsCountedAfterNfcNormalization() {
        // decomposed "ế" is 3 code points, composed it is 1: 8 composed letters + digit are 9 after NFC
        String decomposed = Normalizer.normalize("ếếếếếếếế1", Normalizer.Form.NFD);
        assertThat(decomposed.codePointCount(0, decomposed.length())).isGreaterThan(9);
        assertThat(codes(decomposed)).isEmpty();
        String tooShortAfterNfc = Normalizer.normalize("ếếếếế1", Normalizer.Form.NFD); // 6 code points after NFC
        assertThat(codes(tooShortAfterNfc)).containsExactly("PASSWORD_TOO_SHORT");
    }

    @Test
    void aPasswordWithoutALetterOrWithoutADigitIsRefused() {
        assertThat(codes("12345678")).containsExactly("PASSWORD_NO_LETTER");
        assertThat(codes("abcdefgh")).containsExactly("PASSWORD_NO_DIGIT");
        assertThat(codes("!!!!!!!!")).containsExactlyInAnyOrder("PASSWORD_NO_LETTER", "PASSWORD_NO_DIGIT");
    }

    @Test
    void lettersAndDigitsOfAnyScriptCount() {
        assertThat(codes("Ngưỡng٣٤٥٦")).isEmpty(); // Vietnamese letters, Arabic-Indic digits (\p{Nd})
    }

    @Test
    void severalProblemsAreReportedTogether() {
        assertThat(codes("abc")).containsExactlyInAnyOrder("PASSWORD_TOO_SHORT", "PASSWORD_NO_DIGIT");
    }

    @Test
    void whitespaceIsPartOfThePasswordAndIsNotTrimmed() {
        assertThat(codes("  abc1  ")).isEmpty(); // 8 code points counting the spaces
    }

    @Test
    void aMissingPasswordIsLeftToNotNull() {
        assertThat(codes(null)).isEmpty();
    }

    @Test
    void passwordEqualToTheEmailIsRefusedIgnoringCase() {
        assertThat(registrationErrors("an.nguyen1@mail.com", "an.nguyen1@mail.com", "an.nguyen1@mail.com"))
                .contains("password:PASSWORD_EQUALS_EMAIL");
        assertThat(registrationErrors("an.nguyen1@mail.com", "AN.Nguyen1@Mail.COM", "AN.Nguyen1@Mail.COM"))
                .contains("password:PASSWORD_EQUALS_EMAIL");
        assertThat(registrationErrors("an.nguyen1@mail.com", "other-pass1", "other-pass1")).isEmpty();
    }

    @Test
    void emailIsTrimmedBeforeItIsComparedWithThePassword() {
        assertThat(registrationErrors("  an.nguyen1@mail.com ", "an.nguyen1@mail.com", "an.nguyen1@mail.com"))
                .contains("password:PASSWORD_EQUALS_EMAIL");
    }

    @Test
    void aConfirmationThatDoesNotMatchIsRefusedOnTheConfirmationField() {
        assertThat(registrationErrors("an@mail.com", "abc12345", "abc12346"))
                .containsExactly("confirmPassword:PASSWORD_MISMATCH");
        assertThat(registrationErrors("an@mail.com", "abc12345", " abc12345"))
                .as("the confirmation is compared exactly, spaces included")
                .containsExactly("confirmPassword:PASSWORD_MISMATCH");
    }
}
