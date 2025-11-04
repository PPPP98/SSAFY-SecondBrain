package uknowklp.secondbrain.api.user.domain;

import org.hibernate.validator.constraints.Length;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false)
	private Long id;

	@Column(nullable = false, unique = true, updatable = false)
	private String email;

	@Column(nullable = false)
	private String name;

	@Length(max = 2048)
	private String picture;

	@Column(nullable = false)
	private Boolean setAlarm;

	public User update(String name, String picture) {
		this.name = name;
		this.picture = picture;
		return this;
	}

	public void toggleSetAlarm() {
		this.setAlarm = !this.setAlarm;
	}
}
