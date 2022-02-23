package uk.gov.hmcts.divorce.bulkscan.transformation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.divorce.bulkscan.validation.OcrValidator;
import uk.gov.hmcts.divorce.divorcecase.model.CaseData;
import uk.gov.hmcts.divorce.endpoint.data.OcrValidationResponse;
import uk.gov.hmcts.divorce.endpoint.model.ExceptionRecord;
import uk.gov.hmcts.reform.bsp.common.error.InvalidDataException;
import uk.gov.hmcts.reform.bsp.common.model.shared.in.OcrDataField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.divorce.bulkscan.transformation.D8FormToCaseTransformer.TRANSFORMATION_AND_OCR_WARNINGS;
import static uk.gov.hmcts.divorce.bulkscan.util.FileUtil.loadJson;
import static uk.gov.hmcts.divorce.bulkscan.validation.data.OcrDataFields.transformOcrMapToObject;
import static uk.gov.hmcts.divorce.divorcecase.model.ApplicationType.SOLE_APPLICATION;
import static uk.gov.hmcts.divorce.divorcecase.model.DivorceOrDissolution.DIVORCE;
import static uk.gov.hmcts.divorce.endpoint.data.FormType.D8;
import static uk.gov.hmcts.divorce.testutil.TestDataHelper.inputScannedDocuments;
import static uk.gov.hmcts.divorce.testutil.TestDataHelper.scannedDocuments;

@ExtendWith(MockitoExtension.class)
public class D8FormToCaseTransformerTest {


    @InjectMocks
    private D8FormToCaseTransformer d8FormToCaseTransformer;

    @Mock
    private OcrValidator validator;

    @Mock
    private Applicant1Transformer applicant1Transformer;

    @Mock
    private Applicant2Transformer applicant2Transformer;

    @Mock
    private ApplicationTransformer applicationTransformer;

    @Mock
    private MarriageDetailsTransformer marriageDetailsTransformer;

    @Mock
    private PaperFormDetailsTransformer paperFormDetailsTransformer;

    @Mock
    private ObjectMapper mapper;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void shouldSuccessfullyTransformD8FormWithScannedDocumentsWithoutWarnings() throws Exception {

        String validApplicationOcrJson = loadJson("src/test/resources/transformation/input/valid-d8-form-ocr.json");
        List<OcrDataField> ocrDataFields = MAPPER.readValue(validApplicationOcrJson, new TypeReference<>() {
        });

        final var caseData = CaseData.builder().applicationType(SOLE_APPLICATION).divorceOrDissolution(DIVORCE).build();
        final var transformationDetails =
            TransformationDetails
                .builder()
                .ocrDataFields(transformOcrMapToObject(ocrDataFields))
                .caseData(caseData)
                .build();

        Function<TransformationDetails, TransformationDetails> app1App2 = mock(Function.class);
        Function<TransformationDetails, TransformationDetails> app1App2Application = mock(Function.class);
        Function<TransformationDetails, TransformationDetails> app1App2ApplicationMarriage = mock(Function.class);
        Function<TransformationDetails, TransformationDetails> app1App2ApplicationMarriagePaper = mock(Function.class);

        when(applicant1Transformer.andThen(applicant2Transformer)).thenReturn(app1App2);
        when(app1App2.andThen(applicationTransformer)).thenReturn(app1App2Application);
        when(app1App2Application.andThen(marriageDetailsTransformer)).thenReturn(app1App2ApplicationMarriage);
        when(app1App2ApplicationMarriage.andThen(paperFormDetailsTransformer)).thenReturn(app1App2ApplicationMarriagePaper);
        when(app1App2ApplicationMarriagePaper.apply(any(TransformationDetails.class))).thenReturn(transformationDetails);

        when(validator.validateOcrData(D8.getName(), transformOcrMapToObject(ocrDataFields)))
            .thenReturn(OcrValidationResponse.builder().build());

        Map<String, Object> transformedCaseData = new HashMap<>();
        when(mapper.convertValue(any(CaseData.class), any(TypeReference.class))).thenReturn(transformedCaseData);

        ExceptionRecord exceptionRecord = exceptionRecord(ocrDataFields);
        final var transformedOutput = d8FormToCaseTransformer.transformIntoCaseData(exceptionRecord);

        assertThat(transformedOutput).contains(entry(TRANSFORMATION_AND_OCR_WARNINGS, emptyList()));
        assertThat(transformedOutput.get("scannedDocuments"))
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(scannedDocuments());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSuccessfullyTransformD8FormWithWarnings() throws Exception {

        String invalidApplicationOcrJson = loadJson("src/test/resources/transformation/input/invalid-d8-form-ocr.json");
        List<OcrDataField> ocrDataFields = MAPPER.readValue(invalidApplicationOcrJson, new TypeReference<>() {
        });

        final var caseData = CaseData.builder().applicationType(SOLE_APPLICATION).divorceOrDissolution(DIVORCE).build();
        final var transformationDetails =
            TransformationDetails
                .builder()
                .ocrDataFields(transformOcrMapToObject(ocrDataFields))
                .caseData(caseData)
                .build();

        Function<TransformationDetails, TransformationDetails> app1App2 = mock(Function.class);
        Function<TransformationDetails, TransformationDetails> app1App2Application = mock(Function.class);
        Function<TransformationDetails, TransformationDetails> app1App2ApplicationMarriage = mock(Function.class);
        Function<TransformationDetails, TransformationDetails> app1App2ApplicationMarriagePaper = mock(Function.class);

        when(applicant1Transformer.andThen(applicant2Transformer)).thenReturn(app1App2);
        when(app1App2.andThen(applicationTransformer)).thenReturn(app1App2Application);
        when(app1App2Application.andThen(marriageDetailsTransformer)).thenReturn(app1App2ApplicationMarriage);
        when(app1App2ApplicationMarriage.andThen(paperFormDetailsTransformer)).thenReturn(app1App2ApplicationMarriagePaper);
        when(app1App2ApplicationMarriagePaper.apply(any(TransformationDetails.class))).thenReturn(transformationDetails);

        when(validator.validateOcrData(D8.getName(), transformOcrMapToObject(ocrDataFields)))
            .thenReturn(OcrValidationResponse.builder().build());

        Map<String, Object> transformedCaseData = new HashMap<>();
        when(mapper.convertValue(any(CaseData.class), any(TypeReference.class))).thenReturn(transformedCaseData);

        var exceptionRecord = exceptionRecord(ocrDataFields);
        final var transformedOutput = d8FormToCaseTransformer.transformIntoCaseData(exceptionRecord);
        final List<ListValue<String>> warnings = (List<ListValue<String>>) transformedOutput.get(TRANSFORMATION_AND_OCR_WARNINGS);

        assertThat(warnings)
            .extracting("value")
            .isEqualTo(
                List.of(
                    "Please review divorce type in the scanned form",
                    "Please review application type in the scanned form",
                    "Please review serve out of UK in the scanned form",
                    "Please review respondent by post and applicant will serve application in the scanned form",
                    "Please review respondent address different to service address in the scanned form"
                )
            );
    }

    @Test
    void shouldThrowInvalidDataExceptionWhenOcrValidationContainsErrors() throws Exception {
        String validApplicationOcrJson = loadJson("src/test/resources/transformation/input/valid-d8-form-ocr.json");
        List<OcrDataField> ocrDataFields = MAPPER.readValue(validApplicationOcrJson, new TypeReference<>() {
        });

        when(validator.validateOcrData(D8.getName(), transformOcrMapToObject(ocrDataFields)))
            .thenReturn(OcrValidationResponse.builder().errors(List.of("some error")).build());

        ExceptionRecord exceptionRecord = exceptionRecord(ocrDataFields);

        assertThatThrownBy(() -> d8FormToCaseTransformer.transformIntoCaseData(exceptionRecord))
            .isExactlyInstanceOf(InvalidDataException.class)
            .hasMessageContaining("OCR validation errors")
            .extracting("errors")
            .isEqualTo(List.of("some error"));

    }

    @Test
    void shouldThrowInvalidDataExceptionWhenOcrTransformationThrowsException() throws Exception {
        String validApplicationOcrJson = loadJson("src/test/resources/transformation/input/valid-d8-form-ocr.json");
        List<OcrDataField> ocrDataFields = MAPPER.readValue(validApplicationOcrJson, new TypeReference<>() {
        });

        when(validator.validateOcrData(D8.getName(), transformOcrMapToObject(ocrDataFields)))
            .thenReturn(OcrValidationResponse.builder().build());

        ExceptionRecord exceptionRecord = exceptionRecord(ocrDataFields);

        doThrow(new RuntimeException("some exception")).when(applicant1Transformer).andThen(applicant2Transformer);

        assertThatThrownBy(() -> d8FormToCaseTransformer.transformIntoCaseData(exceptionRecord))
            .isExactlyInstanceOf(InvalidDataException.class)
            .hasMessageContaining("some exception")
            .extracting("errors")
            .isEqualTo(List.of("Some error occurred during D8 Form transformation."));
    }

    private ExceptionRecord exceptionRecord(List<OcrDataField> ocrDataFields) {
        return ExceptionRecord
            .builder()
            .formType(D8.getName())
            .ocrDataFields(ocrDataFields)
            .scannedDocuments(inputScannedDocuments())
            .build();
    }
}
