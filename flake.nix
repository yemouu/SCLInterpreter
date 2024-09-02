{
  description = "SCL Interpreter";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
  };

  outputs = { self, nixpkgs }: {
    devShells.x86_64-linux.default = nixpkgs.legacyPackages.x86_64-linux.mkShell {
      buildInputs = with nixpkgs.legacyPackages.x86_64-linux; [
        google-java-format
        jdk17
        jdt-language-server
        maven
      ];
    };
    packages.x86_64-linux.default = nixpkgs.legacyPackages.x86_64-linux.maven.buildMavenPackage rec {
      pname = "SCLInterpreter";
      version = "1.0.0";
      src = ./.;

      mvnHash = "sha256-8WgNz6qOeBqmT2tfM4puTZ1mUgE0TsqsSTMOtYwHBrc=";
      nativeBuildInputs = with nixpkgs.legacyPackages.x86_64-linux; [ makeWrapper ];

      installPhase = ''
        mkdir -p $out/bin $out/share/${pname}
        install -Dm644 target/scli-${version}.jar $out/share/${pname}

        makeWrapper ${nixpkgs.legacyPackages.x86_64-linux.jre17_minimal}/bin/java $out/bin/scli \
          --add-flags "-jar $out/share/${pname}/scli-${version}.jar"
      '';
    };
    apps.x86_64-linux.default = {
      type = "app";
      program = "${self.packages.x86_64-linux.default}/bin/scli";
    };
  };
}
