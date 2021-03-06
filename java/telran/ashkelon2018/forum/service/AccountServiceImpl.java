package telran.ashkelon2018.forum.service;

import java.time.LocalDateTime;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import telran.ashkelon2018.forum.configuration.AccountConfiguration;
import telran.ashkelon2018.forum.configuration.AccountUserCredentials;
import telran.ashkelon2018.forum.dao.UserAccountRepository;
import telran.ashkelon2018.forum.domain.UserAccount;
import telran.ashkelon2018.forum.dto.UserProfileDto;
import telran.ashkelon2018.forum.dto.UserRegDto;
import telran.ashkelon2018.forum.exceptions.UserConflictException;
import telran.ashkelon2018.forum.exceptions.UserNotAuthorizedExeption;

@Service
public class AccountServiceImpl implements AccountService {
	@Autowired
	UserAccountRepository userRepository;

	@Autowired
	AccountConfiguration accountConfiguration;

	@Override
	public UserProfileDto addUser(UserRegDto userRegDto, String token) {
		AccountUserCredentials credentials = accountConfiguration.tokenDecode(token);
		System.err.println(credentials.getLogin());
		if (userRepository.existsById(credentials.getLogin())) {
			throw new UserConflictException();
		}
		String hashPassword = BCrypt.hashpw(credentials.getPassword(), BCrypt.gensalt());
		UserAccount userAccount  = null;
		if (credentials.getLogin().equals("Admin")) {
			userAccount = UserAccount.builder().login(credentials.getLogin()).password(hashPassword)
					.firstName(userRegDto.getFirstName()).lastName(userRegDto.getLastName()).role("Admin")
					.expdate(LocalDateTime.now().plusDays(accountConfiguration.getExpPeriod())).build();
		} else {
		userAccount = UserAccount.builder().login(credentials.getLogin()).password(hashPassword)
				.firstName(userRegDto.getFirstName()).lastName(userRegDto.getLastName()).role("User")
				.expdate(LocalDateTime.now().plusDays(accountConfiguration.getExpPeriod())).build();}
		userRepository.save(userAccount);
		return convertToUserProfileDto(userAccount);
	}

	private UserProfileDto convertToUserProfileDto(UserAccount userAccount) {
		return UserProfileDto.builder().firstName(userAccount.getFirstName()).lastName(userAccount.getLastName())
				.login(userAccount.getLogin()).roles(userAccount.getRoles()).build();
	}

	@Override
	public UserProfileDto editUser(UserRegDto userRegDto, String token) {
		AccountUserCredentials credentials = accountConfiguration.tokenDecode(token);
		UserAccount userAccount = userRepository.findById(credentials.getLogin()).get();
		if (userRegDto.getFirstName() != null) {
			userAccount.setFirstName(userRegDto.getFirstName());
		}
		if (userRegDto.getLastName() != null) {
			userAccount.setLastName(userRegDto.getLastName());
		}
		userRepository.save(userAccount);
		return convertToUserProfileDto(userAccount);
	}

	@Override
	public UserProfileDto removeUser(String login, String token) {

		AccountUserCredentials userCredentials = accountConfiguration.tokenDecode(token);
		UserAccount userAccount = userRepository.findById(userCredentials.getLogin()).orElse(null);
		if ((!(userAccount.getRoles().contains("Moderator")) && (!userAccount.getRoles().contains("Admin")))) {
			if (!userAccount.getLogin().equals(login)) {
				throw new UserNotAuthorizedExeption();
			}
		}
		UserAccount userAccountToDelete = userRepository.findById(login).orElse(null); 
		if (userAccountToDelete != null) {
			userRepository.delete(userAccountToDelete);
		}
		return convertToUserProfileDto(userAccountToDelete);
	}

	@Override
	public Set<String> addRole(String login, String role, String token) {
		AccountUserCredentials userCredentials = accountConfiguration.tokenDecode(token);
		UserAccount userAccount = userRepository.findById(userCredentials.getLogin()).orElse(null);

		if (!userAccount.getRoles().contains("Admin")) {

			throw new UserNotAuthorizedExeption();
		}

		UserAccount userAccountToChange = userRepository.findById(login).orElse(null);
		if (userAccountToChange != null) {
			userAccountToChange.addRole(role);
			userRepository.save(userAccountToChange);
		} else {
			return null;
		}
		return userAccountToChange.getRoles();
	}

	@Override
	public Set<String> removeRole(String login, String role, String token) {
		AccountUserCredentials userCredentials = accountConfiguration.tokenDecode(token);
		UserAccount userAccount = userRepository.findById(userCredentials.getLogin()).orElse(null);
		if (!userAccount.getRoles().contains("Admin")) {
			throw new UserNotAuthorizedExeption();
		}

		UserAccount userAccountToChange = userRepository.findById(login).orElse(null);
		if (userAccountToChange != null) {
			userAccountToChange.removeRole(role);
			userRepository.save(userAccountToChange);
		} else {
			return null;
		}
		return userAccountToChange.getRoles();
	}

	@Override
	public void changePassword(String password, String token) {
		AccountUserCredentials credentials = accountConfiguration.tokenDecode(token);
		UserAccount userAccount = userRepository.findById(credentials.getLogin()).get();
		String hashPassword = BCrypt.hashpw(password, BCrypt.gensalt());
		userAccount.setPassword(hashPassword);
		userAccount.setExpdate(LocalDateTime.now().plusDays(accountConfiguration.getExpPeriod()));
		userRepository.save(userAccount);
	}

	@Override
	public UserProfileDto login(String token) {
		AccountUserCredentials credentials = accountConfiguration.tokenDecode(token);
		UserAccount userAccount = userRepository.findById(credentials.getLogin()).get();
		return convertToUserProfileDto(userAccount);
	}

}
