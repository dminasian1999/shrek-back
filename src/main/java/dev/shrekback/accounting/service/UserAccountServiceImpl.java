package dev.shrekback.accounting.service;

import dev.shrekback.accounting.dao.UserAccountRepository;
import dev.shrekback.accounting.dao.UserTokenRepository;
import dev.shrekback.accounting.dto.*;
import dev.shrekback.accounting.dto.exceptions.InvalidEmailExeption;
import dev.shrekback.accounting.dto.exceptions.TokenExpiredExeption;
import dev.shrekback.accounting.dto.exceptions.UserExistsException;
import dev.shrekback.accounting.dto.exceptions.UserNotFoundException;
import dev.shrekback.accounting.model.*;
import dev.shrekback.post.dao.PostRepository;
import dev.shrekback.post.model.Post;
import dev.shrekback.post.dto.SizeQuantityDto;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.mongodb.client.result.UpdateResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountRepository userAccountRepository;
    private final UserTokenRepository userTokenRepository;
    private final MongoTemplate mongoTemplate;
    private final PostRepository postRepository;


    // --- User Account Management ---

    @Override
    public void changePassword(String login, String newPassword) {
        validatePassword(newPassword);
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);
        String encoded = passwordEncoder.encode(newPassword);
        updateFields(login, Update.update("password", encoded));
    }

    @Override
    public void changeRolesList(String login, String role, boolean isAddRole) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);

        Role changed = Role.valueOf(role.toUpperCase(java.util.Locale.ROOT));
        updateFields(login, isAddRole ? new Update().addToSet("roles", changed) : new Update().pull("roles", changed));
    }

    private void checkEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,63}$";
        if (email == null || email.length() > 254 || !Pattern.matches(emailRegex, email)) {
            throw new InvalidEmailExeption();
        }
    }

    @Override
    public List<UserDto> getAllUsers() {
        return StreamSupport.stream(userAccountRepository.findAll().spliterator(), false)
                .map(u -> modelMapper.map(u, UserDto.class))
                .toList();
    }

    @Override
    public UserDto getUser(String login) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);
        userAccount.getCart().recalculateTotalPrice();
        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public void recoveryPassword(String token, String newPassword) {
        validatePassword(newPassword);
        UserToken userToken = mongoTemplate.findAndRemove(Query.query(Criteria.where("_id").is(token)), UserToken.class);
        if (userToken == null) throw new TokenExpiredExeption();

        if (LocalDateTime.now().isAfter(userToken.getExpirationDate())) {
            userTokenRepository.delete(userToken);
            throw new TokenExpiredExeption();
        }

        changePassword(userToken.getLogin(), newPassword);
    }

    @Override
    public void recoveryPasswordLink(String login) {
        // No mail transport is configured. Never promise a reset that cannot be delivered.
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Password recovery is currently unavailable. Please contact the shop.");
    }

    @Override
    public UserDto register(UserRegisterDto userRegisterDto) {
        checkEmail(userRegisterDto.getLogin());
        validatePassword(userRegisterDto.getPassword());
        validateName(userRegisterDto.getFirstName());
        validateName(userRegisterDto.getLastName());
        if (userAccountRepository.existsById(userRegisterDto.getLogin())) {
            throw new UserExistsException();
        }
        UserAccount userAccount = modelMapper.map(userRegisterDto, UserAccount.class);
        userAccount.setPassword(passwordEncoder.encode(userRegisterDto.getPassword()));
        try {
            userAccountRepository.insert(userAccount);
        } catch (DuplicateKeyException duplicate) {
            throw new UserExistsException();
        }

        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public UserDto removeUser(String login) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);
        userAccountRepository.delete(userAccount);
        return modelMapper.map(userAccount, UserDto.class);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least 8 characters and at most 72 UTF-8 bytes.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First and last name are required and must be at most 100 characters.");
        }
    }

    @Override
    public UserDto updateUser(String login, UserEditDto userEditDto) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);

        Update update = new Update();
        if (userEditDto.getFirstName() != null) {
            update.set("firstName", userEditDto.getFirstName());
            validateName(userEditDto.getFirstName());
            userAccount.setFirstName(userEditDto.getFirstName());
        }
        if (userEditDto.getLastName() != null) {
            update.set("lastName", userEditDto.getLastName());
            validateName(userEditDto.getLastName());
            userAccount.setLastName(userEditDto.getLastName());
        }

        if (!update.getUpdateObject().isEmpty()) updateFields(login, update);
        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public UserDto updatePaymentMethod(String login, PaymentMethodDto dto) {
        throw new ResponseStatusException(HttpStatus.GONE, "Card details are not stored by this shop. Please use the secure PayPal checkout.");
    }

    @Override
    public UserDto updateAddress(String login, AddressDto dto) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);

        validateField(dto.getFullName(), "Full name", 150, true);
        validateField(dto.getStreet(), "Street address", 250, true);
        validateField(dto.getCity(), "City", 100, true);
        validateField(dto.getState(), "State", 100, false);
        validateField(dto.getZipCode(), "Postal code", 20, true);
        validateField(dto.getCountry(), "Country", 100, true);
        validateField(dto.getPhone(), "Phone", 40, false);
        Address address = new Address();
        address.setFullName(dto.getFullName());
        address.setStreet(dto.getStreet());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setZipCode(dto.getZipCode());
        address.setCountry(dto.getCountry());
        address.setPhone(dto.getPhone());

        userAccount.setAddress(address);
        updateFields(login, Update.update("address", address));

        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public UserDto changeWishList(String login, String productId, boolean isAdd) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);

        if (isAdd && !postRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This product is no longer available.");
        }
        if (isAdd) userAccount.addWishList(productId); else userAccount.removeWishList(productId);
        updateFields(login, isAdd ? new Update().addToSet("wishList", productId) : new Update().pull("wishList", productId));

        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public UserDto changeCartList(String login, Item item, boolean isAdd) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);

        if (item == null || item.getCartItemId() == null || item.getCartItemId().isBlank() || item.getCartItemId().length() > 250) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid cart item is required.");
        }
        if (isAdd) {
            if (item.getProduct() == null || item.getProduct().getId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A product is required.");
            }
            Post product = postRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This product is no longer available."));
            Item existing = userAccount.getCart().getItems().stream()
                    .filter(entry -> entry.getCartItemId().equals(item.getCartItemId())).findFirst().orElse(null);
            if (existing != null && (!existing.getProduct().getId().equals(product.getId())
                    || !Objects.equals(existing.getSelectedSize(), item.getSelectedSize()) && item.getSelectedSize() != null)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart item identity does not match the product.");
            }
            int quantity = item.getQuantity() + (existing == null ? 0 : existing.getQuantity());
            if (item.getQuantity() < 1 || quantity > 99) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be between 1 and 99.");
            }
            String size = resolveSize(product, existing == null ? item.getSelectedSize() : existing.getSelectedSize(), quantity);
            userAccount.getCart().getItems().remove(existing);
            userAccount.getCart().addCartEntry(new Item(item.getCartItemId(), product, size, quantity));
        } else {
            userAccount.getCart().removeCartEntry(item);
        }
        saveCart(userAccount);

        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public UserDto updateCartList(String login, String cartItemId, boolean isAdd) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);

        Item item = userAccount.getCart().getItems().stream().filter(entry -> entry.getCartItemId().equals(cartItemId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found."));
        if (isAdd) {
            Post product = postRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This product is no longer available."));
            resolveSize(product, item.getSelectedSize(), item.getQuantity() + 1);
        }
        userAccount.getCart().updateCartList(cartItemId, isAdd);
        saveCart(userAccount);

        return modelMapper.map(userAccount, UserDto.class);
    }

    private void updateFields(String login, Update update) {
        UpdateResult result = mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(login)), update, UserAccount.class);
        if (result.getMatchedCount() != 1) throw new UserNotFoundException();
    }

    private void saveCart(UserAccount user) {
        Criteria version = user.getCartVersion() == 0
                ? new Criteria().orOperator(Criteria.where("cartVersion").is(0L), Criteria.where("cartVersion").exists(false))
                : Criteria.where("cartVersion").is(user.getCartVersion());
        Query unchanged = Query.query(new Criteria().andOperator(Criteria.where("_id").is(user.getLogin()), version));
        UpdateResult result = mongoTemplate.updateFirst(unchanged,
                Update.update("cart", user.getCart()).inc("cartVersion", 1), UserAccount.class);
        if (result.getMatchedCount() != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Your cart changed in another window. Refresh it and try again.");
        }
    }

    private void validateField(String value, String name, int maxLength, boolean required) {
        if ((required && (value == null || value.isBlank())) || (value != null && value.length() > maxLength)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + " is required and must contain no more than " + maxLength + " characters.");
        }
    }

    private String resolveSize(Post product, String selectedSize, int quantity) {
        if (quantity < 1 || quantity > 99) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be between 1 and 99.");
        List<SizeQuantityDto> variants = product.getSizeQuantities();
        if (variants == null || variants.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This product is currently out of stock.");
        }
        String size = selectedSize;
        if ((size == null || size.isBlank()) && variants.size() == 1) size = variants.get(0).getSize();
        final String requestedSize = size;
        SizeQuantityDto variant = variants.stream().filter(entry -> Objects.equals(entry.getSize(), requestedSize)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please select an available size."));
        if (variant.getQuantity() == null || variant.getQuantity() < quantity) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The requested quantity is not available.");
        }
        return size;
    }

    // Legacy capture is deliberately unavailable: ownership and amount verification live in PayPalService.
    @Override
    public boolean captureOrder(String orderId) {
        throw new ResponseStatusException(HttpStatus.GONE, "Please use the secure checkout flow.");
    }
}
