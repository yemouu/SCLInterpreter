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
    # packages.x86_64-linux.default = self.packages.x86_64-linux.hello;
    # overlays.default = final: prev: { };
  };
}
