package com.starterkit.springboot.brs.controller.v1.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.starterkit.springboot.brs.controller.v1.request.ProfileRequest;
import com.starterkit.springboot.brs.controller.v1.response.LoginResponse;
import com.starterkit.springboot.brs.security.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.starterkit.springboot.brs.security.SecurityConstants.*;

@RestController
@CrossOrigin(maxAge = 36000, origins = "*" , allowedHeaders = "*")
@RequestMapping("/apiauth")
@Api(value = "brs-application", description = "Operations pertaining to user login and logout in the BRS application")
public class JwtAuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;


    @Autowired
    private  org.modelmapper.ModelMapper modelMapper;

    @Autowired
    private CustomUserDetailsService userDetailsService;


    @Autowired
    private  com.starterkit.springboot.brs.service.UserService userService;

//
//    /mybootcamps
//    /myprogress
//    /leaderboard
//    /onlinePractice--> Code --> Compiled bt Judge API



    @PostMapping("/profile")
    @ApiOperation(value = "",  authorizations = {@Authorization(value = "apikey")})
    public  ResponseEntity<?> getProfile(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> list= authentication.getAuthorities();
        String user=(String)authentication.getPrincipal();
        com.starterkit.springboot.brs.dto.model.user.UserDto userDto= new com.starterkit.springboot.brs.dto.model.user.UserDto();
        userDto=  userService.findUserByEmail(user);
        return  ResponseEntity.ok(userDto);
    }

    @RequestMapping(value ="/authenticate", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<?> createAuthToken(@RequestBody @Valid LoginRequest loginRequest)  {
        LoginResponse loginResponse = new LoginResponse();

        try{
            authernticate(loginRequest.getEmail(),loginRequest.getPassword());
        }catch (Exception ee){
            loginResponse.setResponse(ee.getMessage());
            loginResponse.setStatus("error");
            return  ResponseEntity.accepted().body(loginResponse);
        }

        final UserDetails user = userDetailsService.loadUserByUsername(loginRequest.email);
        com.starterkit.springboot.brs.dto.model.user.UserDto userDetail=
        userService.findUserByEmail(loginRequest.email);
        // generate the token
        // return that to the cleint
        String token= "";
        String login = user.getUsername();
        if (login != null && login.length() > 0) {
            Claims claims = Jwts.claims().setSubject(login);
            List<String> roles = new ArrayList<>();
            user.getAuthorities().stream().forEach(authority -> roles.add(authority.getAuthority()));
            claims.put("roles", roles);
            token  = Jwts.builder()
                    .setClaims(claims)
                    .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                    .signWith(SignatureAlgorithm.HS512, SECRET)
                    .compact();
            ;
        }
        loginResponse.setResponse(token);
        loginResponse.setUser(userDetail);
        loginResponse.setStatus("success");
//        return ResponseEntity.ok(TOKEN_PREFIX+token);
        return  ResponseEntity.accepted().body(loginResponse);

    }

    private void authernticate(String email, String password) throws  Exception{
        try{
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email,password));
        }
        catch (DisabledException dE){
            throw  new Exception("User Desabled", dE);
        }
        catch (BadCredentialsException badCredentialsException){
            throw  new Exception("User Desabled", badCredentialsException);
        }

    }


    @Getter
    @Setter
    @Accessors(chain = true)
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LoginRequest {
        @NotNull(message = "{constraints.NotEmpty.message}")
        private String email;
        @NotNull(message = "{constraints.NotEmpty.message}")
        private String password;
    }

}
