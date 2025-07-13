package dev.shrekback.security;//
//import dev.ecomback.accounting.dto.UserDto;
//import dev.ecomback.accounting.dto.UserEditDto;
//import dev.ecomback.accounting.dto.UserRegisterDto;
//import dev.ecomback.accounting.service.UserAccountService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.*;
//
//import java.security.Principal;
//import java.util.Base64;
//import java.util.List;
//
//@RestController
//@RequestMapping("/museum")
//@RequiredArgsConstructor
//@CrossOrigin
//public class UserAccountController  {
//
//    final UserAccountService userAccountService;
//
//
//    @PostMapping("/login")
//    public UserDto login(@RequestHeader("Authorization") String token) {
//        token = token.split(" ")[1];
//        System.out.println("con login");
//        String credentials = new String(Base64.getDecoder().decode(token));
//        return userAccountService.getUser(credentials.split(":")[0]);
//    }
//
//    @PostMapping("/register")
//    public UserDto register(@RequestBody UserRegisterDto userRegisterDto) {
//        System.out.println("con register");
//
//        return userAccountService.register(userRegisterDto);
//
//    }
//    @PutMapping("/user/{login}/role/{role}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void addRole(@PathVariable String login, @PathVariable String role) {
//        userAccountService.changeRolesList(login, role, true);
//    }
//
//    @PutMapping("/password")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void changePassword(Principal principal, @RequestHeader("X-Password") String newPassword) {
//        userAccountService.changePassword(principal.getName(), newPassword);
//    }
//
//    @DeleteMapping("/user/{login}/role/{role}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void deleteRole(@PathVariable String login, @PathVariable String role) {
//        userAccountService.changeRolesList(login, role, false);
//    }
//
//    @GetMapping("/users")
//    public List<UserDto> getAllUsers() {
//        return userAccountService.getAllUsers();
//    }
//
//
//
//    @PostMapping("/recovery/{token}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void recoveryPassword(@PathVariable String token, @RequestHeader("X-Password") String newPassword) {
//        userAccountService.recoveryPassword(token, newPassword);
//    }
//
//    @GetMapping("/recovery/{login}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void recoveryPasswordLink(@PathVariable String login) {
//        userAccountService.recoveryPasswordLink(login);
//    }
//
//
//
//    @DeleteMapping("/user/{login}")
//    public UserDto removeUser(@PathVariable String login) {
//        return userAccountService.removeUser(login);
//    }
//
//
//    @PutMapping("/user")
//    public UserDto updateUser(Principal principal, @RequestBody UserEditDto userEditDto) {
//        return userAccountService.updateUser(principal.getName(), userEditDto);
//    }
//
//
//
//
//
//}
