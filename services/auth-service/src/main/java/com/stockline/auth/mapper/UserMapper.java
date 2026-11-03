package com.stockline.auth.mapper;

import com.stockline.auth.dto.UserResponse;
import com.stockline.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

	/** Every field except the password hash, which never leaves the service. */
	public UserResponse toResponse(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getUserType(),
				user.getStatus(), user.getCreatedAt(), user.getCreatedBy(), user.getUpdatedAt(), user.getUpdatedBy());
	}
}
