package uk.gov.hmcts.divorce.sow014;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping( path = "/ccd" )
public class CaseController {

    @Autowired
    private JdbcTemplate db;

    @GetMapping({"/cases/{caseRef}"})
    public String getCase(@PathVariable("caseRef") long caseRef) {
        return db.queryForObject(
            "select row_to_json(c) r from case_data c where reference = ?",
            new Object[]{ caseRef }, String.class);
    }
}
