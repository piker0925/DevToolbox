package com.back.tool.devops;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DockerComposeModuleTest {

    private static final YAMLMapper YAML = new YAMLMapper();

    /**
     * 출력 YAML을 파싱해 services.&lt;name&gt; 서비스 맵을 반환한다.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> service(ToolResult result, String name) throws Exception {
        Map<String, Object> root = YAML.readValue(result.textResult(), Map.class);
        Map<String, Object> services = (Map<String, Object>) root.get("services");
        assertThat(services).containsKey(name);
        return (Map<String, Object>) services.get(name);
    }

    @Test
    void convertsBasicDockerRun() throws Exception {
        DockerComposeModule module = new DockerComposeModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("command", "docker run -p 8080:80 nginx")
        ));

        assertThat(result.isFile()).isFalse();
        // 서비스 이름은 container_name이 없으면 이미지명에서 파생된다.
        Map<String, Object> svc = service(result, "nginx");
        assertThat(svc.get("image")).isEqualTo("nginx");
        assertThat(svc.get("ports")).isEqualTo(List.of("8080:80"));
    }

    @Test
    void mapsEveryFlagToCorrectComposeKey() throws Exception {
        DockerComposeModule module = new DockerComposeModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("command",
                "docker run --name myapp -p 3000:3000 -e DB_HOST=localhost "
                        + "-v /data:/app/data --network bridge --restart always "
                        + "myimage:latest --spring.profiles.active=prod")
        ));

        // YAML 구조를 파싱해 각 플래그가 정확한 compose 키로 매핑됐는지 검증한다.
        // (env→environment 같은 키를 틀리거나 -v/--network/--restart/trailing command를
        //  누락하는 뮤턴트를 잡는다 — 이전엔 -p/-e/--name만 부분 문자열로 확인했다.)
        Map<String, Object> svc = service(result, "myapp");
        assertThat(svc.get("image")).isEqualTo("myimage:latest");
        assertThat(svc.get("container_name")).isEqualTo("myapp");
        assertThat(svc.get("ports")).isEqualTo(List.of("3000:3000"));
        assertThat(svc.get("environment")).isEqualTo(List.of("DB_HOST=localhost"));
        assertThat(svc.get("volumes")).isEqualTo(List.of("/data:/app/data"));
        assertThat(svc.get("network_mode")).isEqualTo("bridge");
        assertThat(svc.get("restart")).isEqualTo("always");
        assertThat(svc.get("command")).isEqualTo("--spring.profiles.active=prod");
    }

    @Test
    void moduleMetadata() {
        DockerComposeModule module = new DockerComposeModule();
        assertThat(module.getId()).isEqualTo("docker-compose");
        assertThat(module.isHeavy()).isFalse();
        assertThat(module.getCategory()).isEqualTo("devops");
    }
}
