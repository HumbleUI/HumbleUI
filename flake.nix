{
  description = "HumbleUI - Clojure Desktop UI framework";
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/release-25.11";
  inputs.flake-parts.url = "github:hercules-ci/flake-parts";
  outputs =
    inputs:
    inputs.flake-parts.lib.mkFlake { inherit inputs; } {
      systems = [
        "aarch64-darwin"
        "aarch64-linux"
        "x86_64-darwin"
        "x86_64-linux"
      ];
      perSystem =
        { pkgs, ... }:
        {
          devShells.default = pkgs.mkShell {
            nativeBuildInputs = with pkgs; [
              javaPackages.compiler.openjdk25
              python3
            ];
            LD_LIBRARY_PATH = "${pkgs.libGL}/lib/:$LD_LIBRARY_PATH";
            shellHook = ''
              ./script/repl.py
            '';
          };
        };
    };
}
