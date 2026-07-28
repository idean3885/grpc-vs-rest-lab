package io.github.idean3885.grpclab.profile.memory;

import io.github.idean3885.grpclab.profile.domains.Profile;
import io.github.idean3885.grpclab.profile.ports.ProfileRepository;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 인메모리 프로필 저장소.
 *
 * <p>DB 를 두지 않은 이유는 조회 지연을 없애기 위해서다. DB 왕복이 끼면 프로토콜 계층의 차이가 그 지연에 묻혀 측정되지 않는다. 대신 이 랩의 수치는 "프로토콜
 * 오버헤드의 상한"으로만 읽어야 한다. 실제 서비스에서는 DB 지연이 지배적이므로 프로토콜 교체 효과가 이보다 작게 나타난다.
 */
@Slf4j
@Repository
public class InMemoryProfileRepository implements ProfileRepository {

  /** 미리 만들어 두는 데이터 건수. 벤치마크의 최대 페이로드 크기를 결정한다. */
  private static final int DATASET_SIZE = 10_000;

  private static final String[] REGIONS = {"역삼동", "삼성동", "판교동", "서초동", "잠실동"};

  private final List<Profile> dataset = new ArrayList<>(DATASET_SIZE);
  private final Map<String, Profile> index = new HashMap<>(DATASET_SIZE);

  @PostConstruct
  void loadDataset() {
    // 고정 시드로 만들어 실행마다 같은 페이로드가 나오게 한다.
    for (int i = 0; i < DATASET_SIZE; i++) {
      var profile =
          new Profile(
              "user-%05d".formatted(i),
              "사용자%05d".formatted(i),
              REGIONS[i % REGIONS.length],
              i % 101,
              1_700_000_000_000L + (i * 1_000L));
      dataset.add(profile);
      index.put(profile.userId(), profile);
    }
    log.info("[memory] profile dataset loaded : size={}", dataset.size());
  }

  @Override
  public Optional<Profile> findById(String userId) {
    return Optional.ofNullable(index.get(userId));
  }

  @Override
  public List<Profile> findAll(int size) {
    var bounded = Math.min(Math.max(size, 0), dataset.size());
    return dataset.subList(0, bounded);
  }
}
