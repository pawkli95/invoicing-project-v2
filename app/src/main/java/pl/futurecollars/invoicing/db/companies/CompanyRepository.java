package pl.futurecollars.invoicing.db.companies;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.futurecollars.invoicing.model.Company;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
}