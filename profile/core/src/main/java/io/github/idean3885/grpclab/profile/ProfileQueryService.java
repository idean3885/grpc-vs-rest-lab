package io.github.idean3885.grpclab.profile;

import io.github.idean3885.grpclab.profile.data.ProfileData;
import io.github.idean3885.grpclab.profile.data.ProfileListData;
import io.github.idean3885.grpclab.profile.exception.ProfileNotFoundException;
import io.github.idean3885.grpclab.profile.mappers.ProfileMapper;
import io.github.idean3885.grpclab.profile.ports.ProfileRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 프로필 조회 유스케이스.
 *
 * <p>REST 컨트롤러와 gRPC 서비스가 이 한 곳을 호출한다. 프로토콜별로 분기하는 코드가 없다.
 */
@RequiredArgsConstructor
@Service
public class ProfileQueryService implements ProfileQueryUseCase {

  private final ProfileRepository profileRepository;

  @Override
  public ProfileData getBy(String userId) {
    return profileRepository
        .findById(userId)
        .map(ProfileMapper::toData)
        .orElseThrow(() -> new ProfileNotFoundException(userId));
  }

  @Override
  public ProfileListData listBy(int size) {
    List<ProfileData> profiles =
        profileRepository.findAll(size).stream().map(ProfileMapper::toData).toList();
    return ProfileListData.from(profiles);
  }
}
